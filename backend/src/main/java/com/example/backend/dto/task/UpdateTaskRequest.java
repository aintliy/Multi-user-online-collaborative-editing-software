package com.example.backend.dto.task;

import lombok.Data;

import java.time.LocalDate;

/**
 * 更新任务请求DTO
 */
@Data
public class UpdateTaskRequest {
    
    private String title;
    private String description;
    private String status; // todo / doing / done
    private String priority; // LOW / MEDIUM / HIGH
    private Long assigneeId;
    private LocalDate dueDate;
}
