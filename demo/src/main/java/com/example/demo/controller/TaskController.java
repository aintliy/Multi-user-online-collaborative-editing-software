package com.example.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.common.Result;
import com.example.demo.dto.*;
import com.example.demo.security.JwtUtil;
import com.example.demo.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TaskController {
    
    private final TaskService taskService;
    private final JwtUtil jwtUtil;
    
    /**
     * 在文档中创建任务
     */
    @PostMapping("/documents/{documentId}/tasks")
    public Result<TaskVO> createTask(
            @PathVariable Long documentId,
            @Validated @RequestBody CreateTaskRequest request,
            HttpServletRequest httpRequest) {
        Long userId = jwtUtil.getUserIdFromRequest(httpRequest);
        TaskVO task = taskService.createTask(userId, documentId, request);
        return Result.success(task);
    }
    
    /**
     * 获取文档的任务列表
     */
    @GetMapping("/documents/{documentId}/tasks")
    public Result<List<TaskVO>> getTasksByDocument(
            @PathVariable Long documentId,
            HttpServletRequest httpRequest) {
        Long userId = jwtUtil.getUserIdFromRequest(httpRequest);
        List<TaskVO> tasks = taskService.getTasksByDocument(userId, documentId);
        return Result.success(tasks);
    }
    
    /**
     * 获取我的任务列表（分页）
     */
    @GetMapping("/tasks/my")
    public Result<IPage<TaskVO>> getMyTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            HttpServletRequest httpRequest) {
        Long userId = jwtUtil.getUserIdFromRequest(httpRequest);
        IPage<TaskVO> tasks = taskService.getMyTasks(userId, page, size, status);
        return Result.success(tasks);
    }
    
    /**
     * 更新任务
     */
    @PutMapping("/tasks/{taskId}")
    public Result<TaskVO> updateTask(
            @PathVariable Long taskId,
            @RequestBody UpdateTaskRequest request,
            HttpServletRequest httpRequest) {
        Long userId = jwtUtil.getUserIdFromRequest(httpRequest);
        TaskVO task = taskService.updateTask(userId, taskId, request);
        return Result.success(task);
    }
    
    /**
     * 删除任务
     */
    @DeleteMapping("/tasks/{taskId}")
    public Result<Void> deleteTask(
            @PathVariable Long taskId,
            HttpServletRequest httpRequest) {
        Long userId = jwtUtil.getUserIdFromRequest(httpRequest);
        taskService.deleteTask(userId, taskId);
        return Result.success(null);
    }
}
