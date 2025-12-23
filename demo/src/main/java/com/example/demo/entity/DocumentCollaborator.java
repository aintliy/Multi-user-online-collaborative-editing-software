package com.example.demo.entity;

import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 文档协作者实体（工作区成员）
 * 对应数据库表：document_collaborators
 */
@Data
@TableName("document_collaborators")
public class DocumentCollaborator {
    
    /**
     * 协作者记录ID（主键）
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 文档ID
     */
    @TableField("document_id")
    private Long documentId;
    
    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;
    
    /**
     * 角色：EDITOR-可编辑，VIEWER-只读
     */
    @TableField("role")
    private String role;
    
    /**
     * 邀请人ID（通常为文档所有者）
     */
    @TableField("invited_by")
    private Long invitedBy;
    
    /**
     * 创建时间
     */
    @TableField("created_at")
    private OffsetDateTime createdAt;
    
    /**
     * 被移除时间（踢出工作区）
     */
    @TableField("removed_at")
    private OffsetDateTime removedAt;
}
