package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.dto.PageResponse;
import com.example.demo.dto.UserVO;
import com.example.demo.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员控制器
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    
    private final AdminService adminService;
    
    /**
     * 获取用户列表
     */
    @GetMapping("/users")
    public Result<PageResponse<UserVO>> getUsers(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {
        PageResponse<UserVO> users = adminService.getUsers(page, pageSize, keyword, role, status);
        return Result.success(users);
    }
    
    /**
     * 更新用户角色
     */
    @PutMapping("/users/{userId}/role")
    public Result<Void> updateUserRole(
            @AuthenticationPrincipal Long adminId,
            @PathVariable Long userId,
            @RequestParam String role) {
        adminService.updateUserRole(adminId, userId, role);
        return Result.success();
    }
    
    /**
     * 更新用户状态
     */
    @PutMapping("/users/{userId}/status")
    public Result<Void> updateUserStatus(
            @AuthenticationPrincipal Long adminId,
            @PathVariable Long userId,
            @RequestParam String status) {
        adminService.updateUserStatus(adminId, userId, status);
        return Result.success();
    }
    
    /**
     * 删除用户
     */
    @DeleteMapping("/users/{userId}")
    public Result<Void> deleteUser(
            @AuthenticationPrincipal Long adminId,
            @PathVariable Long userId) {
        adminService.deleteUser(adminId, userId);
        return Result.success();
    }
    
    /**
     * 重置用户密码
     */
    @PostMapping("/users/{userId}/reset-password")
    public Result<Void> resetUserPassword(
            @AuthenticationPrincipal Long adminId,
            @PathVariable Long userId,
            @RequestParam String newPassword) {
        adminService.resetUserPassword(adminId, userId, newPassword);
        return Result.success();
    }
    
    /**
     * 获取系统统计
     */
    @GetMapping("/stats")
    public Result<AdminService.SystemStatsVO> getSystemStats() {
        AdminService.SystemStatsVO stats = adminService.getSystemStats();
        return Result.success(stats);
    }
}
