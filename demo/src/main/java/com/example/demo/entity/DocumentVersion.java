package com.example.demo.entity;

import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 文档版本实体（Git风格提交记录）
 * 对应数据库表：document_versions
 */
@Data
@TableName("document_versions")
public class DocumentVersion {
    
    /**
     * 版本ID（主键）
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 文档ID
     */
    @TableField("document_id")
    private Long documentId;
    
    /**
     * 文档内部版本号（从1开始递增）
     */
    @TableField("version_no")
    private Integer versionNo;
    
    /**
     * 本次提交的完整内容快照
     */
    @TableField("content")
    private String content;
    
    /**
     * 提交说明（类似Git commit message）
     */
    @TableField("commit_message")
    private String commitMessage;
    
    /**
     * 发起提交的用户ID
     */
    @TableField("created_by")
    private Long createdBy;
    
    /**
     * 创建时间
     */
    @TableField("created_at")
    private OffsetDateTime createdAt;
}
