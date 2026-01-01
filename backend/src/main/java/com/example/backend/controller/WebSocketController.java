package com.example.backend.controller;

import java.security.Principal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import com.example.backend.dto.websocket.CursorPosition;
import com.example.backend.dto.websocket.DocumentOperation;
import com.example.backend.dto.websocket.WebSocketMessage;
import com.example.backend.entity.ChatMessage;
import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.ChatService;
import com.example.backend.service.CollaborationCacheService;
import com.example.backend.service.DocumentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
        private final DocumentService documentService;
        private final CollaborationCacheService collaborationCacheService;
    
    // 存储每个文档的光标位置
    private final Map<Long, Map<Long, CursorPosition>> documentCursors = new ConcurrentHashMap<>();

        private static final String TYPE_DRAFT_EDIT = "DRAFT_EDIT";
        private static final String TYPE_SAVE_CONFIRMED = "SAVE_CONFIRMED";
        private static final String TYPE_SAVE_REJECTED = "SAVE_REJECTED";
    
    /**
     * 加入文档协作
     */
    @MessageMapping("/document/{documentId}/join")
    public void joinDocument(@DestinationVariable Long documentId, Principal principal) {
        if (principal == null) return;
        
        User user = userRepository.findByEmail(principal.getName()).orElse(null);
        if (user == null) return;

        // 初始化确认态（无则回填数据库内容）
        initializeConfirmedCache(documentId, user.getId());

        // 添加在线用户到 Redis
        collaborationCacheService.addOnlineUser(documentId, user.getId());
        Set<Long> onlineUsers = collaborationCacheService.getOnlineUsers(documentId);

        WebSocketMessage message = WebSocketMessage.builder()
            .type("JOIN")
            .documentId(documentId)
            .userId(user.getId())
            .nickname(user.getUsername())
            .timestamp(System.currentTimeMillis())
            .data(Map.of(
                "user", userSummary(user),
                "onlineUsers", onlineUsers,
                "onlineUserSummaries", buildOnlineUserSummaries(onlineUsers)
            ))
            .build();

        messagingTemplate.convertAndSend("/topic/document/" + documentId, message);
        sendOnlineSnapshot(documentId, user, onlineUsers);
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
        collaborationCacheService.removeOnlineUser(documentId, user.getId());
        
        // 移除光标
        Map<Long, CursorPosition> cursors = documentCursors.get(documentId);
        if (cursors != null) {
            cursors.remove(user.getId());
        }
        
        WebSocketMessage message = WebSocketMessage.builder()
                .type("LEAVE")
                .documentId(documentId)
                .userId(user.getId())
                .nickname(user.getUsername())
                .timestamp(System.currentTimeMillis())
                .data(Map.of(
                        "userId", user.getId(),
                        "onlineUsers", collaborationCacheService.getOnlineUsers(documentId)
                ))
                .build();

        messagingTemplate.convertAndSend("/topic/document/" + documentId, message);

        // 所有人离开后清空该文档的协作缓存
        if (collaborationCacheService.getOnlineUsers(documentId).isEmpty()) {
            collaborationCacheService.clearDocumentState(documentId);
        }
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

        handleDraftMessage(documentId, user, operation);
    }

    /**
     * 文档草稿编辑（虚字层）
     */
    @MessageMapping("/document/{documentId}/draft")
    public void handleDraftEdit(@DestinationVariable Long documentId,
                                @Payload DocumentOperation operation,
                                Principal principal) {
        if (principal == null) return;

        User user = userRepository.findByEmail(principal.getName()).orElse(null);
        if (user == null) return;

        handleDraftMessage(documentId, user, operation);
    }

    private void handleDraftMessage(Long documentId, User user, DocumentOperation operation) {
        documentService.getEditableDocument(documentId, user.getId());
        String content = operation.getContent();
        if (content == null || content.isEmpty()) {
            content = operation.getText();
        }
        if (content == null) {
            return;
        }

        collaborationCacheService.saveDraft(documentId, user.getId(), content);

        WebSocketMessage message = WebSocketMessage.builder()
                .type(TYPE_DRAFT_EDIT)
                .documentId(documentId)
                .userId(user.getId())
                .nickname(user.getUsername())
                .data(Map.of("content", content))
                .timestamp(System.currentTimeMillis())
                .build();

        messagingTemplate.convertAndSend("/topic/document/" + documentId, message);
    }

    /**
     * 保存确认态（实字层，写入 Redis confirmed）
     */
    @MessageMapping("/document/{documentId}/save")
    public void handleSave(@DestinationVariable Long documentId,
                           @Payload DocumentOperation operation,
                           Principal principal) {
        if (principal == null) return;

        User user = userRepository.findByEmail(principal.getName()).orElse(null);
        if (user == null) return;

        documentService.getEditableDocument(documentId, user.getId());

        String content = operation.getContent();
        if (content == null || content.isEmpty()) {
            content = operation.getText();
        }
        if (content == null) {
            return;
        }

        String lockToken = UUID.randomUUID().toString();
        boolean locked = collaborationCacheService.acquireSaveLock(documentId, lockToken);
        if (!locked) {
            WebSocketMessage rejected = WebSocketMessage.builder()
                    .type(TYPE_SAVE_REJECTED)
                    .documentId(documentId)
                    .userId(user.getId())
                    .nickname(user.getUsername())
                    .data(Map.of("reason", "保存冲突，请稍后重试"))
                    .timestamp(System.currentTimeMillis())
                    .build();
            messagingTemplate.convertAndSendToUser(user.getEmail(), "/queue/notifications", rejected);
            return;
        }

        try {
            collaborationCacheService.saveConfirmed(documentId, content);
            collaborationCacheService.clearDraft(documentId, user.getId());

            WebSocketMessage message = WebSocketMessage.builder()
                    .type(TYPE_SAVE_CONFIRMED)
                    .documentId(documentId)
                    .userId(user.getId())
                    .nickname(user.getUsername())
                    .data(Map.of("content", content))
                    .timestamp(System.currentTimeMillis())
                    .build();

            messagingTemplate.convertAndSend("/topic/document/" + documentId, message);
        } finally {
            collaborationCacheService.releaseSaveLock(documentId, lockToken);
        }
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
        Set<Long> users = collaborationCacheService.getOnlineUsers(documentId);
        Map<Long, CursorPosition> cursors = documentCursors.getOrDefault(documentId, Map.of());
        
        return WebSocketMessage.builder()
                .type("ONLINE_USERS")
                .documentId(documentId)
                .data(Map.of("onlineUsers", users, "cursors", cursors))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    private void initializeConfirmedCache(Long documentId, Long userId) {
        String confirmed = collaborationCacheService.getConfirmed(documentId);
        if (confirmed != null) {
            return;
        }
        try {
            var document = documentService.getDocument(documentId, userId);
            String content = document.getContent() == null ? "" : document.getContent();
            collaborationCacheService.saveConfirmed(documentId, content);
        } catch (Exception e) {
            log.warn("初始化确认态失败 doc:{} user:{}", documentId, userId, e);
        }
    }

    private void sendOnlineSnapshot(Long documentId, User user, Set<Long> onlineUsers) {
        WebSocketMessage snapshot = WebSocketMessage.builder()
                .type("ONLINE_USERS")
                .documentId(documentId)
                .userId(user.getId())
                .nickname(user.getUsername())
            .data(Map.of(
                "onlineUsers", onlineUsers,
                "onlineUserSummaries", buildOnlineUserSummaries(onlineUsers)
            ))
                .timestamp(System.currentTimeMillis())
                .build();
        messagingTemplate.convertAndSendToUser(user.getEmail(), "/queue/online-users", snapshot);
    }

    private Map<String, Object> userSummary(User user) {
        return Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "avatarUrl", user.getAvatarUrl() == null ? "" : user.getAvatarUrl()
        );
    }

    private Set<Map<String, Object>> buildOnlineUserSummaries(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Set.of();
        }
        return userRepository.findAllById(userIds).stream()
                .map(this::userSummary)
                .collect(Collectors.toSet());
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
