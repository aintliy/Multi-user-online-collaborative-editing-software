package com.example.demo.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

/**
 * 用户视图对象（不包含敏感信息）
 */
@Data
public class UserVO {

    private Long id;

    private String username;

    private String email;

    private String phone;

    private String avatarUrl;

    private String profile;

    private String status;
    
    private List<String> roles;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
