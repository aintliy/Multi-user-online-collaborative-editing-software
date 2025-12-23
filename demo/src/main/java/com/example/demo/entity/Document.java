package com.example.demo.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 文档实体（重构版）
 * 对应数据库表：documents
 */
@Data
@TableName("documents")
public class Document {
    
    /**
     * 文档ID（主键）
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 文档标题
     */
    @TableField("title")
    private String title;
    
    /**
     * 所有者用户ID
     */
    @TableField("owner_id")
    private Long ownerId;
    
    /**
     * 当前最新「已提交」内容
     */
    @TableField("content")
    private String content;
    
    /**
     * 文档类型：markdown / docx / txt
     */
    @TableField("doc_type")
    private String docType;
    
    /**
     * 可见性：private-私有，public-公开
     */
    @TableField("visibility")
    private String visibility;
    
    /**
     * 标签（逗号分隔）
     */
    @TableField("tags")
    private String tags;
    
    /**
     * 文件夹ID（可选）
     */
    @TableField("folder_id")
    private String folderId;
    
    /**
     * 状态：active-正常，deleted-已删除
     */
    @TableField("status")
    private String status;
    
    /**
     * 克隆来源文档ID（若为克隆副本）
     */
    @TableField("forked_from_id")
    private Long forkedFromId;
    
    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
