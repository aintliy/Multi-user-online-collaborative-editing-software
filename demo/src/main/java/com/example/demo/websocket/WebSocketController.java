package com.example.demo.websocket;

import java.security.Principal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import com.example.demo.common.BusinessException;
import com.example.demo.entity.User;
import com.example.demo.mapper.UserMapper;
import com.example.demo.service.DocumentService;
import com.example.demo.websocket.dto.CursorPosition;
import com.example.demo.websocket.dto.EditOperation;
import com.example.demo.websocket.dto.OnlineUser;
import com.example.demo.websocket.dto.WebSocketMessage;

/**
 * WebSocket 控制器
 * 处理实时协作相关的消息
 */
@Controller
public class WebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketController.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private OnlineUserManager onlineUserManager;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private UserMapper userMapper;

    /**
     * 用户订阅文档频道时触发（加入文档）
     */
    @SubscribeMapping("/topic/document/{docId}")
    public void subscribeDocument(@DestinationVariable Long docId, Principal principal) {
        if (principal == null) {
            return;
        }

        Long userId = Long.valueOf(principal.getName());
        
        try {
            // 检查用户是否有权限访问文档
            documentService.getDocumentById(docId, userId);

            // 用户加入文档
            List<OnlineUser> onlineUsers = onlineUserManager.joinDocument(docId, userId);

            User user = userMapper.selectById(userId);
            
            // 广播用户加入消息
            WebSocketMessage joinMessage = new WebSocketMessage(
                    "JOIN",
                    docId,
                    userId,
                    user.getUsername(),
                    onlineUsers
            );
            
            messagingTemplate.convertAndSend("/topic/document/" + docId, joinMessage);

            logger.info("用户 {} 加入文档 {}", userId, docId);
        } catch (BusinessException e) {
            logger.warn("用户 {} 无权访问文档 {}", userId, docId);
        }
    }

    /**
     * 处理编辑操作
     */
    @MessageMapping("/document/{docId}/edit")
    public void handleEdit(@DestinationVariable Long docId,
                          @Payload EditOperation operation,
                          Principal principal) {
        if (principal == null) {
            return;
        }

        Long userId = Long.valueOf(principal.getName());
        User user = userMapper.selectById(userId);

        // 广播编辑操作给其他用户
        WebSocketMessage message = new WebSocketMessage(
                "EDIT",
                docId,
                userId,
                user.getUsername(),
                operation
        );

        messagingTemplate.convertAndSend("/topic/document/" + docId, message);

        logger.debug("用户 {} 编辑文档 {}: {}", userId, docId, operation.getOperation());
    }

    /**
     * 处理光标移动
     */
    @MessageMapping("/document/{docId}/cursor")
    public void handleCursor(@DestinationVariable Long docId,
                            @Payload CursorPosition cursor,
                            Principal principal) {
        if (principal == null) {
            return;
        }

        Long userId = Long.valueOf(principal.getName());
        User user = userMapper.selectById(userId);

        // 设置光标颜色
        cursor.setColor(onlineUserManager.getUserColor(docId, userId));

        // 广播光标位置给其他用户
        WebSocketMessage message = new WebSocketMessage(
                "CURSOR",
                docId,
                userId,
                user.getUsername(),
                cursor
        );

        messagingTemplate.convertAndSend("/topic/document/" + docId, message);
    }

    /**
     * 处理聊天消息
     */
    @MessageMapping("/document/{docId}/chat")
    public void handleChat(@DestinationVariable Long docId,
                          @Payload String content,
                          Principal principal) {
        if (principal == null) {
            return;
        }

        Long userId = Long.valueOf(principal.getName());
        User user = userMapper.selectById(userId);

        // 广播聊天消息
        WebSocketMessage message = new WebSocketMessage(
                "CHAT",
                docId,
                userId,
                user.getUsername(),
                content
        );

        messagingTemplate.convertAndSend("/topic/document/" + docId, message);

        logger.info("用户 {} 在文档 {} 发送聊天消息", userId, docId);
    }

    /**
     * 用户离开文档
     */
    @MessageMapping("/document/{docId}/leave")
    public void handleLeave(@DestinationVariable Long docId, Principal principal) {
        if (principal == null) {
            return;
        }

        Long userId = Long.valueOf(principal.getName());
        User user = userMapper.selectById(userId);

        // 用户离开文档
        List<OnlineUser> onlineUsers = onlineUserManager.leaveDocument(docId, userId);

        // 广播用户离开消息
        WebSocketMessage leaveMessage = new WebSocketMessage(
                "LEAVE",
                docId,
                userId,
                user.getUsername(),
                onlineUsers
        );

        messagingTemplate.convertAndSend("/topic/document/" + docId, leaveMessage);

        logger.info("用户 {} 离开文档 {}", userId, docId);
    }

    /**
     * 获取在线用户列表
     */
    @MessageMapping("/document/{docId}/online-users")
    public void getOnlineUsers(@DestinationVariable Long docId, Principal principal) {
        if (principal == null) {
            return;
        }

        Long userId = Long.valueOf(principal.getName());
        List<OnlineUser> onlineUsers = onlineUserManager.getOnlineUsers(docId);

        // 发送在线用户列表给请求者
        WebSocketMessage message = new WebSocketMessage(
                "ONLINE_USERS",
                docId,
                userId,
                null,
                onlineUsers
        );

        messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/online-users",
                message
        );
    }
}
