package com.example.backend.service;

import com.example.backend.dto.PageResponse;
import com.example.backend.dto.friend.FriendMessageDTO;
import com.example.backend.dto.friend.SendMessageRequest;
import com.example.backend.entity.*;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 好友消息服务
 */
@Service
@RequiredArgsConstructor
public class FriendMessageService {
    
    private final FriendMessageRepository messageRepository;
    private final UserFriendRepository friendRepository;
    private final DocumentShareLinkRepository shareLinkRepository;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * 发送消息给好友
     */
    @Transactional
    public FriendMessageDTO sendMessage(Long senderId, SendMessageRequest request) {
        // 验证是否是好友
        if (!friendRepository.areFriends(senderId, request.getReceiverId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能给好友发送消息");
        }
        
        User sender = userService.getUserById(senderId);
        User receiver = userService.getUserById(request.getReceiverId());
        
        FriendMessage message = FriendMessage.builder()
                .sender(sender)
                .receiver(receiver)
                .content(request.getContent())
                .messageType(request.getMessageType() != null ? request.getMessageType() : "TEXT")
                .shareLinkId(request.getShareLinkId())
                .build();
        
        message = messageRepository.save(message);
        
        FriendMessageDTO dto = FriendMessageDTO.fromEntity(message);
        
        // 如果是分享链接消息，附加分享链接信息
        if ("SHARE_LINK".equals(request.getMessageType()) && request.getShareLinkId() != null) {
            shareLinkRepository.findById(request.getShareLinkId()).ifPresent(link -> {
                dto.setShareLinkInfo(FriendMessageDTO.ShareLinkInfo.builder()
                        .documentId(link.getDocument().getId())
                        .documentTitle(link.getDocument().getTitle())
                        .isUsed(link.getIsUsed())
                        .isExpired(link.getExpiresAt().isBefore(LocalDateTime.now()))
                        .build());
            });
        }
        
        // 向接收方发送实时通知
        sendRealTimeNotification(receiver, sender, dto);
        
        return dto;
    }
    
    /**
     * 发送实时好友消息通知
     */
    private void sendRealTimeNotification(User receiver, User sender, FriendMessageDTO message) {
        Map<String, Object> notification = Map.of(
                "type", "FRIEND_MESSAGE",
                "senderId", sender.getId(),
                "senderName", sender.getUsername(),
                "senderAvatar", sender.getAvatarUrl() != null ? sender.getAvatarUrl() : "",
                "message", message,
                "timestamp", System.currentTimeMillis()
        );
        
        // 通过 WebSocket 发送到用户的个人队列
        messagingTemplate.convertAndSendToUser(
                receiver.getEmail(),
                "/queue/notifications",
                notification
        );
    }
    
    /**
     * 获取与某好友的聊天记录
     */
    public List<FriendMessageDTO> getMessages(Long userId, Long friendId) {
        // 验证是否是好友
        if (!friendRepository.areFriends(userId, friendId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能查看与好友的聊天记录");
        }
        
        List<FriendMessage> messages = messageRepository.findMessagesBetweenUsers(userId, friendId);
        return messages.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 分页获取与某好友的聊天记录
     */
    public PageResponse<FriendMessageDTO> getMessagesPaged(Long userId, Long friendId, int page, int pageSize) {
        if (!friendRepository.areFriends(userId, friendId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能查看与好友的聊天记录");
        }
        
        Page<FriendMessage> messagePage = messageRepository.findMessagesBetweenUsers(
                userId, friendId, 
                PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        
        List<FriendMessageDTO> items = messagePage.getContent().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        
        return PageResponse.<FriendMessageDTO>builder()
                .items(items)
                .page(page)
                .pageSize(pageSize)
                .total(messagePage.getTotalElements())
                .build();
    }
    
    /**
     * 标记消息为已读
     */
    @Transactional
    public void markAsRead(Long userId, Long friendId) {
        messageRepository.markMessagesAsRead(friendId, userId);
    }
    
    /**
     * 获取未读消息数量
     */
    public Long getUnreadCount(Long userId) {
        return messageRepository.countUnreadMessages(userId);
    }
    
    /**
     * 获取与某好友的未读消息数量
     */
    public Long getUnreadCountFromFriend(Long userId, Long friendId) {
        return messageRepository.countUnreadMessagesFromFriend(friendId, userId);
    }
    
    private FriendMessageDTO toDTO(FriendMessage message) {
        FriendMessageDTO dto = FriendMessageDTO.fromEntity(message);
        
        if ("SHARE_LINK".equals(message.getMessageType()) && message.getShareLinkId() != null) {
            shareLinkRepository.findById(message.getShareLinkId()).ifPresent(link -> {
                dto.setShareLinkInfo(FriendMessageDTO.ShareLinkInfo.builder()
                        .documentId(link.getDocument().getId())
                        .documentTitle(link.getDocument().getTitle())
                        .isUsed(link.getIsUsed())
                        .isExpired(link.getExpiresAt().isBefore(LocalDateTime.now()))
                        .build());
            });
        }
        
        return dto;
    }
}
