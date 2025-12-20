package com.example.demo.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 通知实体
 */
@Data
@TableName("notifications")
public class Notification {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long receiverId;

    private String type;  // COMMENT / TASK / PERMISSION

    private Long referenceId;  // 关联业务实体 ID

    private String content;

    private Boolean isRead;

    private LocalDateTime createdAt;
}
