package com.example.demo.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统监控控制器（重构版）
 */
@RestController
@RequestMapping("/api/monitor")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class MonitorController {
    
    private final AdminService adminService;
    
    /**
     * 获取系统健康状态
     */
    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("OK");
    }
    
    /**
     * 获取系统统计信息
     */
    @GetMapping("/stats")
    public ApiResponse<AdminService.SystemStatsVO> getStats() {
        AdminService.SystemStatsVO stats = adminService.getSystemStats();
        return ApiResponse.success(stats);
    }
}
