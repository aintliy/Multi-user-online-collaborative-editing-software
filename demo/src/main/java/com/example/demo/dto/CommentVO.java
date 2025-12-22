package com.example.demo.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 评论视图对象
 */
@Data
public class CommentVO {
    
    private Long id;
    
    private Long documentId;
    
    private Long userId;
    
    private String username;
    
    private String avatarUrl;
    
    private String content;
    
    private Long replyToCommentId;
    
    private String rangeInfo;
    
    private String status; // open / resolved
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
