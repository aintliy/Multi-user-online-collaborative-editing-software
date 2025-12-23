package com.example.demo.entity;

import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 通知实体
 * 对应数据库表：notifications
 */
@Data
@TableName("notifications")
public class Notification {
    
    /**
     * 通知ID（主键）
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 接收人ID
     */
    @TableField("receiver_id")
    private Long receiverId;
    
    /**
     * 通知类型：COMMENT-评论，TASK-任务，FRIEND_REQUEST-好友请求，WORKSPACE_REQUEST-协作申请等
     */
    @TableField("type")
    private String type;
    
    /**
     * 关联业务实体ID
     */
    @TableField("reference_id")
    private Long referenceId;
    
    /**
     * 通知内容
     */
    @TableField("content")
    private String content;
    
    /**
     * 是否已读
     */
    @TableField("is_read")
    private Boolean isRead;
    
    /**
     * 创建时间
     */
    @TableField("created_at")
    private OffsetDateTime createdAt;
}
