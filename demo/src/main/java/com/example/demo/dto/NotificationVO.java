package com.example.demo.dto;

import lombok.Data;
import java.time.LocalDateTime;

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
}