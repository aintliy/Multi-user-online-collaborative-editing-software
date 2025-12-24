package com.example.backend.dto.auth;

import com.example.backend.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户信息DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    
    private Long id;
    private String publicId;
    private String username;
    private String email;
    private String phone;
    private String avatarUrl;
    private String profile;
    private String role;
    private String status;
    
    public static UserDTO fromEntity(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .publicId(user.getPublicId())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .profile(user.getProfile())
                .role(user.getRole())
                .status(user.getStatus())
                .build();
    }
}
