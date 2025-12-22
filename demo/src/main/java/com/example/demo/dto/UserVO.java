package com.example.demo.dto;

import java.time.LocalDateTime;

import lombok.Data;
/**
 * 用户视图对象
 */
@Data
public class UserVO {
    private Long id;
    /**
     * 对外公开的用户ID
     */
    private String publicId;
    private String username;
    private String email;
    private String phone;
    private String avatarUrl;
    private String profile;
    /**
     * 系统角色：ADMIN / USER
     */
    private String role;
    private String status;
    private LocalDateTime createdAt;
}