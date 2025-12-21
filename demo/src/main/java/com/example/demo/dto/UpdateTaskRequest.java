package com.example.demo.dto;

import java.time.LocalDate;

import lombok.Data;

@Data
public class UpdateTaskRequest {
    private String status;
    
    private String title;
    
    private String description;
    
    private Long assigneeId;
    
    private LocalDate dueDate;
}
