package com.example.backend.controller;

import com.example.backend.dto.ApiResponse;
import com.example.backend.dto.PageResponse;
import com.example.backend.dto.notification.NotificationDTO;
import com.example.backend.entity.User;
import com.example.backend.service.NotificationService;
import com.example.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 通知控制器
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    
    private final NotificationService notificationService;
    private final UserService userService;
    
    /**
     * 获取通知列表
     */
    @GetMapping
    public ApiResponse<PageResponse<NotificationDTO>> getNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        PageResponse<NotificationDTO> response = notificationService.getNotifications(user.getId(), isRead, page, pageSize);
        return ApiResponse.success(response);
    }
    
    /**
     * 标记通知为已读
     */
    @PostMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(@AuthenticationPrincipal UserDetails userDetails,
                                         @PathVariable Long id) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        notificationService.markAsRead(id, user.getId());
        return ApiResponse.success("标记成功");
    }
    
    /**
     * 获取未读通知数量
     */
    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> getUnreadCount(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        long count = notificationService.getUnreadCount(user.getId());
        return ApiResponse.success(Map.of("count", count));
    }
}
