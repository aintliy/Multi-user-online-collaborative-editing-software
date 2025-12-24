package com.example.backend.service;

import com.example.backend.dto.notification.NotificationDTO;
import com.example.backend.dto.PageResponse;
import com.example.backend.entity.Notification;
import com.example.backend.entity.User;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 通知服务
 */
@Service
@RequiredArgsConstructor
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final UserService userService;
    
    /**
     * 创建通知
     */
    @Transactional
    public void createNotification(Long receiverId, String type, Long referenceId, String content) {
        User receiver = userService.getUserById(receiverId);
        
        Notification notification = Notification.builder()
                .receiver(receiver)
                .type(type)
                .referenceId(referenceId)
                .content(content)
                .isRead(false)
                .build();
        
        notificationRepository.save(notification);
    }
    
    /**
     * 获取通知列表
     */
    public PageResponse<NotificationDTO> getNotifications(Long userId, Boolean isRead, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        Page<Notification> notificationPage;
        
        if (isRead != null) {
            notificationPage = notificationRepository.findByReceiverIdAndIsReadOrderByCreatedAtDesc(userId, isRead, pageable);
        } else {
            notificationPage = notificationRepository.findByReceiverIdOrderByCreatedAtDesc(userId, pageable);
        }
        
        List<NotificationDTO> items = notificationPage.getContent().stream()
                .map(NotificationDTO::fromEntity)
                .collect(Collectors.toList());
        
        return PageResponse.<NotificationDTO>builder()
                .items(items)
                .page(page)
                .pageSize(pageSize)
                .total(notificationPage.getTotalElements())
                .build();
    }
    
    /**
     * 标记通知为已读
     */
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "通知不存在"));
        
        if (!notification.getReceiver().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此通知");
        }
        
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }
    
    /**
     * 获取未读通知数量
     */
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByReceiverIdAndIsRead(userId, false);
    }
}
