package com.example.backend.dto.friend;

import com.example.backend.entity.UserFriend;
import com.example.backend.dto.auth.UserDTO;
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
    private UserDTO user;
    private UserDTO friend;
    private String status;
    private LocalDateTime createdAt;
    
    /**
     * 从实体创建DTO
     * 直接映射user和friend字段，不做转换
     * user字段对应发起好友请求的用户
     * friend字段对应接收好友请求的用户
     */
    public static FriendDTO fromEntity(UserFriend userFriend) {
        return FriendDTO.builder()
                .id(userFriend.getId())
                .user(UserDTO.fromEntity(userFriend.getUser()))
                .friend(UserDTO.fromEntity(userFriend.getFriend()))
                .status(userFriend.getStatus())
                .createdAt(userFriend.getCreatedAt())
                .build();
    }
}
