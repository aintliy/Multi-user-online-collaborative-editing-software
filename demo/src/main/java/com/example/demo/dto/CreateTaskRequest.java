package com.example.demo.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 创建任务请求DTO
 */
@Data
public class CreateTaskRequest {
    
    @NotBlank(message = "任务标题不能为空")
    private String title;
    
    private String description;
    
    @NotNull(message = "指派人ID不能为空")
    private Long assigneeId;
    
    private LocalDate dueDate;
}
