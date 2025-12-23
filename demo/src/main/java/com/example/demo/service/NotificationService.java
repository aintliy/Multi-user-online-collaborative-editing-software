package com.example.demo.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.common.BusinessException;
import com.example.demo.common.ErrorCode;
import com.example.demo.dto.NotificationVO;
import com.example.demo.entity.Notification;
import com.example.demo.mapper.NotificationMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 通知服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    
    private final NotificationMapper notificationMapper;
    
    /**
     * 创建通知
     */
    public void createNotification(Long userId, String type, String content, Long relatedId) {
        Notification notification = new Notification();
        notification.setReceiverId(userId);
        notification.setType(type);
        notification.setContent(content);
        notification.setReferenceId(relatedId);
        notification.setIsRead(false);
        notification.setCreatedAt(OffsetDateTime.now());
        
        notificationMapper.insert(notification);
        
        log.info("创建通知: userId={}, type={}, content={}", userId, type, content);
    }
    
    /**
     * 获取用户通知列表
     */
    public List<NotificationVO> getUserNotifications(Long userId, Boolean isRead, Integer limit) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getReceiverId, userId);
        
        if (isRead != null) {
            wrapper.eq(Notification::getIsRead, isRead);
        }
        
        wrapper.orderByDesc(Notification::getCreatedAt);
        
        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + limit);
        }
        
        List<Notification> notifications = notificationMapper.selectList(wrapper);
        
        return notifications.stream()
            .map(this::convertToNotificationVO)
            .collect(Collectors.toList());
    }
    
    /**
     * 标记通知为已读
     */
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null) {
            throw new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        
        // 检查通知所有权
        if (!notification.getReceiverId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOTIFICATION_NO_PERMISSION);
        }
        
        if (!notification.getIsRead()) {
            notification.setIsRead(true);
            notificationMapper.updateById(notification);
            
            log.info("标记通知已读: notificationId={}, userId={}", notificationId, userId);
        }
    }
    
    /**
     * 批量标记已读
     */
    @Transactional(rollbackFor = Exception.class)
    public void markAllAsRead(Long userId) {
        List<Notification> unreadNotifications = notificationMapper.selectList(
            new LambdaQueryWrapper<Notification>()
                .eq(Notification::getReceiverId, userId)
                .eq(Notification::getIsRead, false)
        );
        
        for (Notification notification : unreadNotifications) {
            notification.setIsRead(true);
            notificationMapper.updateById(notification);
        }
        
        log.info("批量标记已读: userId={}, count={}", userId, unreadNotifications.size());
    }
    
    /**
     * 删除通知
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null) {
            throw new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        
        // 检查通知所有权
        if (!notification.getReceiverId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOTIFICATION_NO_PERMISSION);
        }
        
        notificationMapper.deleteById(notificationId);
        
        log.info("删除通知: notificationId={}, userId={}", notificationId, userId);
    }
    
    /**
     * 获取未读通知数量
     */
    public Long getUnreadCount(Long userId) {
        return notificationMapper.selectCount(
            new LambdaQueryWrapper<Notification>()
                .eq(Notification::getReceiverId, userId)
                .eq(Notification::getIsRead, false)
        );
    }
    
    /**
     * 转换为NotificationVO
     */
    private NotificationVO convertToNotificationVO(Notification notification) {
        NotificationVO vo = new NotificationVO();
        vo.setId(notification.getId());
        vo.setReceiverId(notification.getReceiverId());
        vo.setType(notification.getType());
        vo.setContent(notification.getContent());
        vo.setReferenceId(notification.getReferenceId());
        vo.setIsRead    (notification.getIsRead());
        vo.setCreatedAt(notification.getCreatedAt());
        return vo;
    }
}
