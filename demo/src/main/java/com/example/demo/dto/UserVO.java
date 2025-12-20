package com.example.demo.dto;

import lombok.Data;
import java.time.LocalDateTime;

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

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
