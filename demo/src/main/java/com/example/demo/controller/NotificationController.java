package com.example.demo.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.dto.NotificationVO;
import com.example.demo.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通知控制器
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    
    private final NotificationService notificationService;
    
    /**
     * 获取用户通知列表
     */
    @GetMapping
    public ApiResponse<List<NotificationVO>> getNotifications(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) Integer limit) {
        List<NotificationVO> notifications = notificationService.getUserNotifications(
                userId, isRead, limit);
        return ApiResponse.success(notifications);
    }
    
    /**
     * 获取未读通知数量
     */
    @GetMapping("/unread-count")
    public ApiResponse<Long> getUnreadCount(@AuthenticationPrincipal Long userId) {
        Long count = notificationService.getUnreadCount(userId);
        return ApiResponse.success(count);
    }
    
    /**
     * 标记通知为已读
     */
    @PutMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        notificationService.markAsRead(id, userId);
        return ApiResponse.success();
    }
    
    /**
     * 批量标记已读
     */
    @PutMapping("/read-all")
    public ApiResponse<Void> markAllAsRead(@AuthenticationPrincipal Long userId) {
        notificationService.markAllAsRead(userId);
        return ApiResponse.success();
    }
    
    /**
     * 删除通知
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteNotification(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        notificationService.deleteNotification(id, userId);
        return ApiResponse.success();
    }
}
