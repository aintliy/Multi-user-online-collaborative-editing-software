package com.example.demo.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 聊天消息实体（文档内聊天，可选持久化）
 * 对应数据库表：chat_messages
 */
@Data
@TableName("chat_messages")
public class ChatMessage {
    
    /**
     * 聊天消息ID（主键）
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 文档ID
     */
    @TableField("document_id")
    private Long documentId;
    
    /**
     * 发送人ID
     */
    @TableField("sender_id")
    private Long senderId;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
