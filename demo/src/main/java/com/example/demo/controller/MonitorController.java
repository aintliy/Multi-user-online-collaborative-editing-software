package com.example.demo.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.common.Result;
import com.example.demo.entity.OperationLog;
import com.example.demo.mapper.DocumentMapper;
import com.example.demo.mapper.UserMapper;
import com.example.demo.service.OperationLogService;
import com.example.demo.websocket.OnlineUserManager;

/**
 * 系统监控控制器
 */
@RestController
@RequestMapping("/api/admin/monitor")
@PreAuthorize("hasRole('ADMIN')")
public class MonitorController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DocumentMapper documentMapper;

    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private OnlineUserManager onlineUserManager;

    /**
     * 获取系统统计信息
     * GET /api/admin/monitor/stats
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getSystemStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 用户统计
        long totalUsers = userMapper.selectCount(null);
        stats.put("totalUsers", totalUsers);
        
        // 文档统计
        long totalDocuments = documentMapper.selectCount(null);
        stats.put("totalDocuments", totalDocuments);
        
        // 在线用户数（从OnlineUserManager获取）
        int onlineUsers = onlineUserManager.getTotalOnlineCount();
        stats.put("onlineUsers", onlineUsers);
        
        // 活跃文档数（有用户在线编辑的文档）
        int activeDocuments = onlineUserManager.getActiveDocumentCount();
        stats.put("activeDocuments", activeDocuments);
        
        return Result.success(stats);
    }

    /**
     * 获取操作日志列表（分页）
     * GET /api/admin/monitor/operation-logs
     */
    @GetMapping("/operation-logs")
    public Result<IPage<OperationLog>> getOperationLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        IPage<OperationLog> logs = operationLogService.getOperationLogs(
            page, size, userId, action, targetType, startDate, endDate);
        
        return Result.success(logs);
    }

    /**
     * 获取系统健康状态
     * GET /api/admin/monitor/health
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> getHealthStatus() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // 检查数据库连接
            userMapper.selectCount(null);
            health.put("database", "UP");
        } catch (Exception e) {
            health.put("database", "DOWN");
            health.put("databaseError", e.getMessage());
        }
        
        // 系统资源信息
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        health.put("memory", Map.of(
            "total", totalMemory / (1024 * 1024) + " MB",
            "used", usedMemory / (1024 * 1024) + " MB",
            "free", freeMemory / (1024 * 1024) + " MB"
        ));
        
        health.put("status", "UP");
        
        return Result.success(health);
    }
}
