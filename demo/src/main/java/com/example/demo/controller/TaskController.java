package com.example.demo.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.dto.CreateTaskRequest;
import com.example.demo.dto.PageResponse;
import com.example.demo.dto.TaskVO;
import com.example.demo.dto.UpdateTaskRequest;
import com.example.demo.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 任务控制器（重构版）
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {
    
    private final TaskService taskService;
    
    /**
     * 创建任务
     */
    @PostMapping
    public ApiResponse<TaskVO> createTask(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateTaskRequest request) {
        TaskVO task = taskService.createTask(userId, request);
        return ApiResponse.success(task);
    }
    
    /**
     * 获取任务详情
     */
    @GetMapping("/{id}")
    public ApiResponse<TaskVO> getTask(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        TaskVO task = taskService.getTaskById(id, userId);
        return ApiResponse.success(task);
    }
    
    /**
     * 更新任务
     */
    @PutMapping("/{id}")
    public ApiResponse<TaskVO> updateTask(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateTaskRequest request) {
        TaskVO task = taskService.updateTask(id, userId, request);
        return ApiResponse.success(task);
    }
    
    /**
     * 删除任务
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteTask(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        taskService.deleteTask(id, userId);
        return ApiResponse.success();
    }
    
    /**
     * 获取任务列表
     */
    @GetMapping
    public ApiResponse<PageResponse<TaskVO>> getTasks(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) Long creatorId,
            @RequestParam(required = false) Long relatedDocId) {
        PageResponse<TaskVO> tasks = taskService.getTasks(
                userId, page, pageSize, status, priority, assigneeId, creatorId, relatedDocId);
        return ApiResponse.success(tasks);
    }
}
