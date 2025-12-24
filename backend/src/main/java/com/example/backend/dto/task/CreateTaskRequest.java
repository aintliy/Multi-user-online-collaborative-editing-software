package com.example.backend.dto.task;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

/**
 * 创建任务请求DTO
 */
@Data
public class CreateTaskRequest {
    
    @NotBlank(message = "任务标题不能为空")
    private String title;
    
    private String description;
    
    private Long assigneeId;
    
    private LocalDate dueDate;
    
    private String priority = "MEDIUM";
}
