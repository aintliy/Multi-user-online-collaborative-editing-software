package com.example.demo.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.common.Result;
import com.example.demo.dto.NotificationVO;
import com.example.demo.service.NotificationService;

import lombok.RequiredArgsConstructor;

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
    public Result<List<NotificationVO>> getNotifications(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) Integer limit) {
        List<NotificationVO> notifications = notificationService.getUserNotifications(
                userId, isRead, limit);
        return Result.success(notifications);
    }
    
    /**
     * 获取未读通知数量
     */
    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount(@AuthenticationPrincipal Long userId) {
        Long count = notificationService.getUnreadCount(userId);
        return Result.success(count);
    }
    
    /**
     * 标记通知为已读
     */
    @PutMapping("/{id}/read")
    public Result<Void> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        notificationService.markAsRead(id, userId);
        return Result.success();
    }
    
    /**
     * 批量标记已读
     */
    @PutMapping("/read-all")
    public Result<Void> markAllAsRead(@AuthenticationPrincipal Long userId) {
        notificationService.markAllAsRead(userId);
        return Result.success();
    }
    
    /**
     * 删除通知
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteNotification(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        notificationService.deleteNotification(id, userId);
        return Result.success();
    }
}
