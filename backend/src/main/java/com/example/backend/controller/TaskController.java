package com.example.backend.controller;

import com.example.backend.dto.ApiResponse;
import com.example.backend.dto.task.CreateTaskRequest;
import com.example.backend.dto.task.TaskDTO;
import com.example.backend.dto.task.UpdateTaskRequest;
import com.example.backend.entity.User;
import com.example.backend.service.TaskService;
import com.example.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 任务控制器
 */
@RestController
@RequiredArgsConstructor
public class TaskController {
    
    private final TaskService taskService;
    private final UserService userService;
    
    /**
     * 创建任务
     */
    @PostMapping("/api/documents/{documentId}/tasks")
    public ApiResponse<TaskDTO> createTask(@AuthenticationPrincipal UserDetails userDetails,
                                            @PathVariable Long documentId,
                                            @Valid @RequestBody CreateTaskRequest request) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        TaskDTO task = taskService.createTask(documentId, user.getId(), request);
        return ApiResponse.success("创建成功", task);
    }
    
    /**
     * 获取文档任务列表
     */
    @GetMapping("/api/documents/{documentId}/tasks")
    public ApiResponse<List<TaskDTO>> getDocumentTasks(@AuthenticationPrincipal UserDetails userDetails,
                                                        @PathVariable Long documentId) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        List<TaskDTO> tasks = taskService.getDocumentTasks(documentId, user.getId());
        return ApiResponse.success(tasks);
    }
    
    /**
     * 更新任务
     */
    @PutMapping("/api/tasks/{taskId}")
    public ApiResponse<TaskDTO> updateTask(@AuthenticationPrincipal UserDetails userDetails,
                                            @PathVariable Long taskId,
                                            @RequestBody UpdateTaskRequest request) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        TaskDTO task = taskService.updateTask(taskId, user.getId(), request);
        return ApiResponse.success("更新成功", task);
    }
    
    /**
     * 删除任务
     */
    @DeleteMapping("/api/tasks/{taskId}")
    public ApiResponse<Void> deleteTask(@AuthenticationPrincipal UserDetails userDetails,
                                         @PathVariable Long taskId) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        taskService.deleteTask(taskId, user.getId());
        return ApiResponse.success("删除成功");
    }
}
