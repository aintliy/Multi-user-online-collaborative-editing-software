package com.example.demo.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.common.Result;
import com.example.demo.service.AdminService;

import lombok.RequiredArgsConstructor;

/**
 * 系统监控控制器
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
    public Result<String> health() {
        return Result.success("OK");
    }
    
    /**
     * 获取系统统计信息
     */
    @GetMapping("/stats")
    public Result<AdminService.SystemStatsVO> getStats() {
        AdminService.SystemStatsVO stats = adminService.getSystemStats();
        return Result.success(stats);
    }
}
