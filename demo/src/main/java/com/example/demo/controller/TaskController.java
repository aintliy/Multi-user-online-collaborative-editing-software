package com.example.demo.controller;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.common.Result;
import com.example.demo.dto.CreateTaskRequest;
import com.example.demo.dto.TaskVO;
import com.example.demo.dto.UpdateTaskRequest;
import com.example.demo.security.JwtUtil;
import com.example.demo.service.TaskService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TaskController {
    
    private final TaskService taskService;
    private final JwtUtil jwtUtil;
    
    /**
     * 创建任务
     */
    @PostMapping("/tasks")
    public Result<TaskVO> createTask(
            @Validated @RequestBody CreateTaskRequest request,
            HttpServletRequest httpRequest) {
        Long userId = jwtUtil.getUserIdFromRequest(httpRequest);
        TaskVO task = taskService.createTask(userId, request.getDocumentId(), request);
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
     * 获取我的任务列表
     */
    @GetMapping("/tasks/my")
    public Result<List<TaskVO>> getMyTasks(
            @RequestParam(required = false) String status,
            HttpServletRequest httpRequest) {
        Long userId = jwtUtil.getUserIdFromRequest(httpRequest);
        List<TaskVO> tasks = taskService.getMyTasksList(userId, status);
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
