package com.example.demo.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 通知视图对象
 */
@Data
public class NotificationVO {

    private Long id;

    private Long receiverId;

    private String type;

    private Long referenceId;

    private String content;

    private Boolean isRead;

    private LocalDateTime createdAt;

    // 扩展信息
    private String senderName;  // 发送者用户名
    
    private String documentTitle;  // 相关文档标题
}
