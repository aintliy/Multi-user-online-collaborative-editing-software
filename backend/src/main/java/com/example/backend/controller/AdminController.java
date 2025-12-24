package com.example.backend.controller;

import com.example.backend.dto.ApiResponse;
import com.example.backend.dto.PageResponse;
import com.example.backend.dto.auth.UserDTO;
import com.example.backend.dto.document.DocumentDTO;
import com.example.backend.entity.OperationLog;
import com.example.backend.entity.User;
import com.example.backend.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理员控制器
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {
    
    private final AdminService adminService;
    
    /**
     * 获取系统统计数据
     */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getStats() {
        Map<String, Object> stats = adminService.getStats();
        return ApiResponse.success(stats);
    }
    
    /**
     * 获取用户列表
     */
    @GetMapping("/users")
    public ApiResponse<PageResponse<UserDTO>> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<UserDTO> response = adminService.getUsers(keyword, status, page, pageSize);
        return ApiResponse.success(response);
    }
    
    /**
     * 禁用用户
     */
    @PostMapping("/users/{userId}/ban")
    public ApiResponse<Void> banUser(@PathVariable Long userId) {
        adminService.banUser(userId);
        return ApiResponse.success("用户已禁用");
    }
    
    /**
     * 解禁用户
     */
    @PostMapping("/users/{userId}/unban")
    public ApiResponse<Void> unbanUser(@PathVariable Long userId) {
        adminService.unbanUser(userId);
        return ApiResponse.success("用户已解禁");
    }
    
    /**
     * 删除用户
     */
    @DeleteMapping("/users/{userId}")
    public ApiResponse<Void> deleteUser(@PathVariable Long userId) {
        adminService.deleteUser(userId);
        return ApiResponse.success("用户已删除");
    }
    
    /**
     * 获取文档列表
     */
    @GetMapping("/documents")
    public ApiResponse<PageResponse<DocumentDTO>> getDocuments(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<DocumentDTO> response = adminService.getDocuments(keyword, page, pageSize);
        return ApiResponse.success(response);
    }
    
    /**
     * 删除文档
     */
    @DeleteMapping("/documents/{documentId}")
    public ApiResponse<Void> deleteDocument(@PathVariable Long documentId) {
        adminService.deleteDocument(documentId);
        return ApiResponse.success("文档已删除");
    }
    
    /**
     * 获取操作日志
     */
    @GetMapping("/operation-logs")
    public ApiResponse<PageResponse<OperationLog>> getOperationLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String operationType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<OperationLog> response = adminService.getOperationLogs(userId, operationType, page, pageSize);
        return ApiResponse.success(response);
    }
}
