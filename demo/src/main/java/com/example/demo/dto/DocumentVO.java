package com.example.demo.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 文档视图对象
 */
@Data
public class DocumentVO {

    private Long id;

    private String title;

    private Long ownerId;

    private String ownerName;  // 所有者用户名

    private String content;

    private String docType;

    private String status;

    private String permission;  // 当前用户对该文档的权限：OWNER / EDITOR / VIEWER

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
