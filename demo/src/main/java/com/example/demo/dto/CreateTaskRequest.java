package com.example.demo.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateTaskRequest {
    @NotBlank(message = "任务标题不能为空")
    private String title;
    
    private String description;
    
    private Long documentId;
    
    @NotNull(message = "分配人不能为空")
    private Long assigneeId;
    
    private LocalDate dueDate;
}
