package com.example.demo.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

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
    
    private String status;
    
    private LocalDate dueDate;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
