package com.example.backend.controller;

import com.example.backend.dto.websocket.CursorPosition;
import com.example.backend.dto.websocket.DocumentOperation;
import com.example.backend.dto.websocket.WebSocketMessage;
import com.example.backend.entity.ChatMessage;
import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket协作控制器
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
    private final UserRepository userRepository;
    
    // 存储每个文档的在线用户
    private final Map<Long, Set<Long>> documentUsers = new ConcurrentHashMap<>();
    // 存储每个文档的光标位置
    private final Map<Long, Map<Long, CursorPosition>> documentCursors = new ConcurrentHashMap<>();
    
    /**
     * 加入文档协作
     */
    @MessageMapping("/document/{documentId}/join")
    public void joinDocument(@DestinationVariable Long documentId, Principal principal) {
        if (principal == null) return;
        
        User user = userRepository.findByEmail(principal.getName()).orElse(null);
        if (user == null) return;
        
        // 添加用户到文档
        documentUsers.computeIfAbsent(documentId, k -> new CopyOnWriteArraySet<>()).add(user.getId());
        
        // 广播用户加入消息
        WebSocketMessage message = WebSocketMessage.builder()
                .type("JOIN")
                .documentId(documentId)
                .userId(user.getId())
                .nickname(user.getUsername())
                .timestamp(System.currentTimeMillis())
                .data(Map.of("onlineUsers", documentUsers.get(documentId)))
                .build();
        
        messagingTemplate.convertAndSend("/topic/document/" + documentId, message);
        log.info("用户 {} 加入文档 {} 协作", user.getUsername(), documentId);
    }
    
    /**
     * 离开文档协作
     */
    @MessageMapping("/document/{documentId}/leave")
    public void leaveDocument(@DestinationVariable Long documentId, Principal principal) {
        if (principal == null) return;
        
        User user = userRepository.findByEmail(principal.getName()).orElse(null);
        if (user == null) return;
        
        // 移除用户
        Set<Long> users = documentUsers.get(documentId);
        if (users != null) {
            users.remove(user.getId());
        }
        
        // 移除光标
        Map<Long, CursorPosition> cursors = documentCursors.get(documentId);
        if (cursors != null) {
            cursors.remove(user.getId());
        }
        
        // 广播用户离开消息
        WebSocketMessage message = WebSocketMessage.builder()
                .type("LEAVE")
                .documentId(documentId)
                .userId(user.getId())
                .nickname(user.getUsername())
                .timestamp(System.currentTimeMillis())
                .build();
        
        messagingTemplate.convertAndSend("/topic/document/" + documentId, message);
        log.info("用户 {} 离开文档 {} 协作", user.getUsername(), documentId);
    }
    
    /**
     * 处理文档编辑操作
     */
    @MessageMapping("/document/{documentId}/edit")
    public void handleEdit(@DestinationVariable Long documentId, 
                           @Payload DocumentOperation operation,
                           Principal principal) {
        if (principal == null) return;
        
        User user = userRepository.findByEmail(principal.getName()).orElse(null);
        if (user == null) return;
        
        // 广播编辑操作给其他用户
        WebSocketMessage message = WebSocketMessage.builder()
                .type("EDIT")
                .documentId(documentId)
                .userId(user.getId())
                .nickname(user.getUsername())
                .data(operation)
                .timestamp(System.currentTimeMillis())
                .build();
        
        messagingTemplate.convertAndSend("/topic/document/" + documentId, message);
    }
    
    /**
     * 更新光标位置
     */
    @MessageMapping("/document/{documentId}/cursor")
    public void updateCursor(@DestinationVariable Long documentId,
                             @Payload CursorPosition cursorPosition,
                             Principal principal) {
        if (principal == null) return;
        
        User user = userRepository.findByEmail(principal.getName()).orElse(null);
        if (user == null) return;
        
        // 更新光标位置
        cursorPosition.setUserId(user.getId());
        cursorPosition.setNickname(user.getUsername());
        documentCursors.computeIfAbsent(documentId, k -> new ConcurrentHashMap<>())
                .put(user.getId(), cursorPosition);
        
        // 广播光标位置
        WebSocketMessage message = WebSocketMessage.builder()
                .type("CURSOR")
                .documentId(documentId)
                .userId(user.getId())
                .nickname(user.getUsername())
                .data(cursorPosition)
                .timestamp(System.currentTimeMillis())
                .build();
        
        messagingTemplate.convertAndSend("/topic/document/" + documentId, message);
    }
    
    /**
     * 发送聊天消息
     */
    @MessageMapping("/document/{documentId}/chat")
    public void sendChatMessage(@DestinationVariable Long documentId,
                                @Payload Map<String, String> payload,
                                Principal principal) {
        if (principal == null) return;
        
        User user = userRepository.findByEmail(principal.getName()).orElse(null);
        if (user == null) return;
        
        String content = payload.get("content");
        if (content == null || content.trim().isEmpty()) return;
        
        // 保存聊天消息
        ChatMessage chatMessage = chatService.saveMessage(documentId, user.getId(), content);
        
        // 广播聊天消息
        WebSocketMessage message = WebSocketMessage.builder()
                .type("CHAT")
                .documentId(documentId)
                .userId(user.getId())
                .nickname(user.getUsername())
                .data(Map.of(
                        "id", chatMessage.getId(),
                        "content", content,
                        "userId", user.getId(),
                        "nickname", user.getUsername(),
                        "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : ""
                ))
                .timestamp(System.currentTimeMillis())
                .build();
        
        messagingTemplate.convertAndSend("/topic/document/" + documentId, message);
    }
    
    /**
     * 获取在线用户列表
     */
    @MessageMapping("/document/{documentId}/online-users")
    @SendToUser("/queue/online-users")
    public WebSocketMessage getOnlineUsers(@DestinationVariable Long documentId) {
        Set<Long> users = documentUsers.getOrDefault(documentId, Set.of());
        Map<Long, CursorPosition> cursors = documentCursors.getOrDefault(documentId, Map.of());
        
        return WebSocketMessage.builder()
                .type("ONLINE_USERS")
                .documentId(documentId)
                .data(Map.of("onlineUsers", users, "cursors", cursors))
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * 发送通知给特定用户
     */
    public void sendNotification(Long userId, String type, Object data) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;
        
        WebSocketMessage message = WebSocketMessage.builder()
                .type("NOTIFICATION")
                .userId(userId)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
        
        messagingTemplate.convertAndSendToUser(
                user.getEmail(),
                "/queue/notifications",
                message
        );
    }
}
