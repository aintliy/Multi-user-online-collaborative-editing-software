package com.example.demo.dto;

import java.time.OffsetDateTime;

import lombok.Data;

/**
 * 协作者视图对象
 */
@Data
public class CollaboratorVO {
    private Long id;
    private Long documentId;
    private Long userId;
    private String username;
    private String email;
    private String avatarUrl;
    private String role; // EDITOR / VIEWER
    private OffsetDateTime createdAt;
}