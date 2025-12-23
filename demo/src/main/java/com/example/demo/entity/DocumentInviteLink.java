package com.example.demo.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 文档邀请链接实体（可选增强功能）
 * 对应数据库表：document_invite_links
 */
@Data
@TableName("document_invite_links")
public class DocumentInviteLink {
    
    /**
     * 邀请链接ID（主键）
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 文档ID
     */
    @TableField("document_id")
    private Long documentId;
    
    /**
     * 邀请令牌（随机生成）
     */
    @TableField("token")
    private String token;
    
    /**
     * 通过链接加入后的默认角色：EDITOR / VIEWER
     */
    @TableField("role")
    private String role;
    
    /**
     * 最大使用次数（NULL表示无限制）
     */
    @TableField("max_uses")
    private Integer maxUses;
    
    /**
     * 已使用次数
     */
    @TableField("used_count")
    private Integer usedCount;
    
    /**
     * 过期时间
     */
    @TableField("expires_at")
    private LocalDateTime expiresAt;
    
    /**
     * 创建人ID
     */
    @TableField("created_by")
    private Long createdBy;
    
    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
