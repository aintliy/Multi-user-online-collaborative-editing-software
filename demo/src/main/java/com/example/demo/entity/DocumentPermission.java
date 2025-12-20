package com.example.demo.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 文档权限实体
 */
@Data
@TableName("document_permissions")
public class DocumentPermission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long documentId;

    private Long userId;

    private String role;  // EDITOR / VIEWER

    private LocalDateTime createdAt;
}
