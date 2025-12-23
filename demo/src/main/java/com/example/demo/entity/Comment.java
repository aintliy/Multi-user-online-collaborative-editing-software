package com.example.demo.entity;

import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 评论实体（文档批注与回复）
 * 对应数据库表：comments
 */
@Data
@TableName("comments")
public class Comment {
    
    /**
     * 评论ID（主键）
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 文档ID
     */
    @TableField("document_id")
    private Long documentId;
    
    /**
     * 评论人ID
     */
    @TableField("user_id")
    private Long userId;
    
    /**
     * 评论内容
     */
    @TableField("content")
    private String content;
    
    /**
     * 回复目标评论ID
     */
    @TableField("reply_to_comment_id")
    private Long replyToCommentId;
    
    /**
     * 选中范围信息（JSON格式）
     */
    @TableField("range_info")
    private String rangeInfo;
    
    /**
     * 评论状态：open-未解决，resolved-已解决
     */
    @TableField("status")
    private String status;
    
    /**
     * 创建时间
     */
    @TableField("created_at")
    private OffsetDateTime createdAt;
    
    /**
     * 更新时间
     */
    @TableField("updated_at")
    private OffsetDateTime updatedAt;
}
