package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.common.Result;
import com.example.demo.dto.NotificationVO;
import com.example.demo.service.NotificationService;

/**
 * 通知控制器
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    /**
     * 获取通知列表（分页）
     * GET /api/notifications?page=1&size=10&isRead=false
     */
    @GetMapping
    public Result<IPage<NotificationVO>> getNotifications(@RequestParam(defaultValue = "1") int page,
                                                           @RequestParam(defaultValue = "10") int size,
                                                           @RequestParam(required = false) Boolean isRead,
                                                           Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        IPage<NotificationVO> notifications = notificationService.getNotifications(userId, page, size, isRead);
        return Result.success(notifications);
    }

    /**
     * 获取未读通知数量
     * GET /api/notifications/unread-count
     */
    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        long count = notificationService.getUnreadCount(userId);
        return Result.success(count);
    }

    /**
     * 标记通知为已读
     * PUT /api/notifications/{id}/read
     */
    @PutMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id,
                                   Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        notificationService.markAsRead(userId, id);
        return Result.success("通知已标记为已读", null);
    }

    /**
     * 标记所有通知为已读
     * PUT /api/notifications/read-all
     */
    @PutMapping("/read-all")
    public Result<Void> markAllAsRead(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        notificationService.markAllAsRead(userId);
        return Result.success("所有通知已标记为已读", null);
    }

    /**
     * 删除通知
     * DELETE /api/notifications/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteNotification(@PathVariable Long id,
                                           Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        notificationService.deleteNotification(userId, id);
        return Result.success("通知删除成功", null);
    }
}
