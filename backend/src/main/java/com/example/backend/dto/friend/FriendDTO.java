package com.example.backend.dto.friend;

import com.example.backend.entity.UserFriend;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 好友DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FriendDTO {
    
    private Long id;
    private Long userId;
    private String username;
    private String publicId;
    private String avatarUrl;
    private String status;
    private LocalDateTime createdAt;
    
    public static FriendDTO fromEntity(UserFriend userFriend, boolean isSender) {
        return FriendDTO.builder()
                .id(userFriend.getId())
                .userId(isSender ? userFriend.getFriend().getId() : userFriend.getUser().getId())
                .username(isSender ? userFriend.getFriend().getUsername() : userFriend.getUser().getUsername())
                .publicId(isSender ? userFriend.getFriend().getPublicId() : userFriend.getUser().getPublicId())
                .avatarUrl(isSender ? userFriend.getFriend().getAvatarUrl() : userFriend.getUser().getAvatarUrl())
                .status(userFriend.getStatus())
                .createdAt(userFriend.getCreatedAt())
                .build();
    }
}
