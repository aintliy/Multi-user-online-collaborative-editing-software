package com.example.demo.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 协作者视图对象
 */
@Data
public class CollaboratorVO {
    
    private Long id;
    
    private Long userId;
    
    private String username;
    
    private String avatarUrl;
    
    private String role; // EDITOR / VIEWER
    
    private LocalDateTime createdAt;
}
