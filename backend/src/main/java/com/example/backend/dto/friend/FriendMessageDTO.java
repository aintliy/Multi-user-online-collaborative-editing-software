package com.example.backend.dto.friend;

import com.example.backend.dto.auth.UserDTO;
import com.example.backend.entity.FriendMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 好友消息DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FriendMessageDTO {
    
    private Long id;
    private UserDTO sender;
    private UserDTO receiver;
    private String content;
    private String messageType;
    private Long shareLinkId;
    private Boolean isRead;
    private LocalDateTime createdAt;
    
    // 分享链接相关信息（如果是分享链接消息）
    private ShareLinkInfo shareLinkInfo;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ShareLinkInfo {
        private Long documentId;
        private String documentTitle;
        private Boolean isUsed;
        private Boolean isExpired;
    }
    
    public static FriendMessageDTO fromEntity(FriendMessage message) {
        return FriendMessageDTO.builder()
                .id(message.getId())
                .sender(UserDTO.fromEntity(message.getSender()))
                .receiver(UserDTO.fromEntity(message.getReceiver()))
                .content(message.getContent())
                .messageType(message.getMessageType())
                .shareLinkId(message.getShareLinkId())
                .isRead(message.getIsRead())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
