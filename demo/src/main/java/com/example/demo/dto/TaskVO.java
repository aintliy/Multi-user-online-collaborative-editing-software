package com.example.demo.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 任务视图对象
 */
@Data
public class TaskVO {
    
    private Long id;
    
    private Long documentId;
    
    private String documentTitle;
    
    private Long creatorId;
    
    private String creatorName;
    
    private Long assigneeId;
    
    private String assigneeName;
    
    private String title;
    
    private String description;
    
    private String status; // TODO / DOING / DONE
    
    private LocalDate dueDate;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
