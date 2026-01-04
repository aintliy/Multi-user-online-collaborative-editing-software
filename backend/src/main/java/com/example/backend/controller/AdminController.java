package com.example.backend.controller;

import com.example.backend.dto.ApiResponse;
import com.example.backend.dto.OperationLogDTO;
import com.example.backend.dto.PageResponse;
import com.example.backend.dto.auth.UserDTO;
import com.example.backend.dto.document.DocumentDTO;
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
     * 获取文档列表（排除已删除）
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
     * 获取回收站文档列表
     */
    @GetMapping("/documents/trash")
    public ApiResponse<PageResponse<DocumentDTO>> getDeletedDocuments(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<DocumentDTO> response = adminService.getDeletedDocuments(keyword, page, pageSize);
        return ApiResponse.success(response);
    }
    
    /**
     * 恢复已删除的文档
     */
    @PostMapping("/documents/{documentId}/restore")
    public ApiResponse<Void> restoreDocument(@PathVariable Long documentId) {
        adminService.restoreDocument(documentId);
        return ApiResponse.success("文档已恢复");
    }
    
    /**
     * 删除文档（逻辑删除，移入回收站）
     */
    @DeleteMapping("/documents/{documentId}")
    public ApiResponse<Void> deleteDocument(@PathVariable Long documentId) {
        adminService.deleteDocument(documentId);
        return ApiResponse.success("文档已移入回收站");
    }
    
    /**
     * 永久删除文档（物理删除）
     */
    @DeleteMapping("/documents/{documentId}/permanent")
    public ApiResponse<Void> permanentDeleteDocument(@PathVariable Long documentId) {
        adminService.permanentDeleteDocument(documentId);
        return ApiResponse.success("文档已永久删除");
    }
    
    /**
     * 获取操作日志
     */
    @GetMapping("/operation-logs")
    public ApiResponse<PageResponse<OperationLogDTO>> getOperationLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String operationType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<OperationLogDTO> response = adminService.getOperationLogs(userId, operationType, page, pageSize);
        return ApiResponse.success(response);
    }
}
