package com.example.demo.dto;

import java.time.LocalDate;

import lombok.Data;
/**
 * 更新任务请求DTO
 */
@Data
public class UpdateTaskRequest {
    private String title;
    private String description;
    private String status;  // TODO / DOING / DONE
    private Long assigneeId;
    private LocalDate dueDate;
    private String priority;
}