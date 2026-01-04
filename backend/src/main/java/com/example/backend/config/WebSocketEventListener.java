package com.example.backend.config;

import java.security.Principal;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import com.example.backend.dto.websocket.WebSocketMessage;
import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.CollaborationCacheService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket 事件监听器
 * 处理连接、断开等事件，确保 Redis 数据正确清理
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final CollaborationCacheService collaborationCacheService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // 存储 sessionId -> userId 的映射
    private final Map<String, Long> sessionUserMap = new ConcurrentHashMap<>();
    
    // 存储 sessionId -> documentId 的映射（一个用户一次只能在一个文档中）
    private final Map<String, Long> sessionDocumentMap = new ConcurrentHashMap<>();

    /**
     * 监听 WebSocket 连接事件
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        log.debug("WebSocket 连接建立: sessionId={}", sessionId);
    }

    /**
     * 监听订阅事件，记录用户订阅的文档
     */
    @EventListener
    public void handleSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination();
        
        if (destination != null && destination.startsWith("/topic/document/")) {
            try {
                // 提取文档 ID: /topic/document/{documentId}
                String docIdStr = destination.replace("/topic/document/", "");
                Long documentId = Long.valueOf(docIdStr);
                
                // 获取用户信息
                Principal userPrincipal = headerAccessor.getUser();
                if (userPrincipal != null) {
                    String email = userPrincipal.getName();
                    User user = userRepository.findByEmail(email).orElse(null);
                    if (user != null) {
                        sessionUserMap.put(sessionId, user.getId());
                        sessionDocumentMap.put(sessionId, documentId);
                        log.debug("用户 {} 订阅文档 {}, sessionId={}", user.getUsername(), documentId, sessionId);
                    }
                }
            } catch (NumberFormatException e) {
                log.debug("无法解析文档 ID: {}", destination);
            }
        }
    }

    /**
     * 监听 WebSocket 断开连接事件
     * 清理 Redis 中的在线用户数据
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        Long userId = sessionUserMap.remove(sessionId);
        Long documentId = sessionDocumentMap.remove(sessionId);
        
        if (userId != null && documentId != null) {
            // 从 Redis 移除在线用户
            collaborationCacheService.removeOnlineUser(documentId, userId);
            
            // 获取用户信息用于广播
            User user = userRepository.findById(userId).orElse(null);
            String nickname = user != null ? user.getUsername() : "Unknown";
            
            // 广播用户离开消息
            WebSocketMessage message = WebSocketMessage.builder()
                    .type("LEAVE")
                    .documentId(documentId)
                    .userId(userId)
                    .nickname(nickname)
                    .timestamp(System.currentTimeMillis())
                    .data(Map.of(
                            "userId", userId,
                            "onlineUsers", collaborationCacheService.getOnlineUsers(documentId)
                    ))
                    .build();
            
            messagingTemplate.convertAndSend("/topic/document/" + documentId, message);
            
            // 如果所有人都离开了，清空文档缓存
            Set<Long> remainingUsers = collaborationCacheService.getOnlineUsers(documentId);
            if (remainingUsers.isEmpty()) {
                collaborationCacheService.clearDocumentState(documentId);
                log.info("文档 {} 无在线用户，已清理缓存", documentId);
            }
            
            log.info("用户 {} (sessionId={}) 断开连接，已从文档 {} 移除", nickname, sessionId, documentId);
        } else {
            log.debug("WebSocket 断开: sessionId={}, 无需清理（用户未加入文档）", sessionId);
        }
    }

    /**
     * 注册用户加入文档（供 WebSocketController 调用）
     */
    public void registerUserJoinDocument(String sessionId, Long userId, Long documentId) {
        if (sessionId != null && userId != null && documentId != null) {
            sessionUserMap.put(sessionId, userId);
            sessionDocumentMap.put(sessionId, documentId);
        }
    }

    /**
     * 注销用户离开文档（供 WebSocketController 调用）
     */
    public void unregisterUserFromDocument(String sessionId) {
        if (sessionId != null) {
            sessionUserMap.remove(sessionId);
            sessionDocumentMap.remove(sessionId);
        }
    }
}
