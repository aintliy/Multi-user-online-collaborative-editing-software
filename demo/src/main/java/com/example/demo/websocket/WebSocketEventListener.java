package com.example.demo.websocket;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.example.demo.websocket.dto.OnlineUser;

/**
 * WebSocket 事件监听器
 * 处理连接和断开事件
 */
@Component
public class WebSocketEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    @Autowired
    private OnlineUserManager onlineUserManager;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * 存储 sessionId 和用户正在访问的文档
     * Key: sessionId
     * Value: documentId
     */
    private final Map<String, Long> sessionDocuments = new ConcurrentHashMap<>();

    /**
     * 连接建立事件
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        logger.info("WebSocket 连接建立: sessionId = {}", sessionId);
    }

    /**
     * 连接断开事件
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        Principal principal = headerAccessor.getUser();

        if (principal != null && sessionId != null) {
            Long userId = Long.valueOf(principal.getName());
            Long docId = sessionDocuments.remove(sessionId);

            if (docId != null) {
                // 用户离开文档
                List<OnlineUser> onlineUsers = onlineUserManager.leaveDocument(docId, userId);

                // 广播用户离开消息
                messagingTemplate.convertAndSend(
                        "/topic/document/" + docId,
                        Map.of(
                                "type", "LEAVE",
                                "userId", userId,
                                "docId", docId,
                                "payload", onlineUsers,
                                "timestamp", System.currentTimeMillis()
                        )
                );

                logger.info("用户 {} 断开连接，离开文档 {}", userId, docId);
            }
        }

        logger.info("WebSocket 连接断开: sessionId = {}", sessionId);
    }

    /**
     * 注册会话和文档的关联
     */
    public void registerSessionDocument(String sessionId, Long documentId) {
        sessionDocuments.put(sessionId, documentId);
    }

    /**
     * 取消会话和文档的关联
     */
    public void unregisterSessionDocument(String sessionId) {
        sessionDocuments.remove(sessionId);
    }
}
