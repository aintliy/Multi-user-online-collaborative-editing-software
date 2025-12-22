package com.example.demo.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.common.ApiResponse;
import com.example.demo.dto.CreateTaskRequest;
import com.example.demo.dto.PageResponse;
import com.example.demo.dto.TaskVO;
import com.example.demo.dto.UpdateTaskRequest;
import com.example.demo.service.TaskService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 任务控制器
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
