package com.example.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.dto.NotificationVO;
import com.example.demo.entity.Comment;
import com.example.demo.entity.Document;
import com.example.demo.entity.Notification;
import com.example.demo.mapper.CommentMapper;
import com.example.demo.mapper.DocumentMapper;
import com.example.demo.mapper.NotificationMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 通知服务
 */
@Service
public class NotificationService {

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private DocumentMapper documentMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * 创建通知
     */
    public void createNotification(Long receiverId, String type, Long referenceId, String content) {
        Notification notification = new Notification();
        notification.setReceiverId(receiverId);
        notification.setType(type);
        notification.setReferenceId(referenceId);
        notification.setContent(content);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        notificationMapper.insert(notification);

        // 实时推送通知给用户（通过 WebSocket）
        NotificationVO vo = convertToVO(notification);
        messagingTemplate.convertAndSendToUser(
                String.valueOf(receiverId),
                "/queue/notifications",
                vo
        );
    }

    /**
     * 获取用户的通知列表（分页，支持类型过滤）
     */
    public IPage<NotificationVO> getNotifications(Long userId, int pageNum, int pageSize, Boolean isRead, String type) {
        Page<Notification> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getReceiverId, userId);
        
        if (isRead != null) {
            wrapper.eq(Notification::getIsRead, isRead);
        }
        
        // 按类型筛选
        if (type != null && !type.isEmpty()) {
            wrapper.eq(Notification::getType, type);
        }
        
        wrapper.orderByDesc(Notification::getCreatedAt);

        IPage<Notification> notificationPage = notificationMapper.selectPage(page, wrapper);

        // 转换为 VO
        IPage<NotificationVO> voPage = new Page<>(notificationPage.getCurrent(), notificationPage.getSize(), notificationPage.getTotal());
        List<NotificationVO> voList = notificationPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    /**
     * 获取未读通知数量
     */
    public long getUnreadCount(Long userId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getReceiverId, userId)
               .eq(Notification::getIsRead, false);

        return notificationMapper.selectCount(wrapper);
    }

    /**
     * 标记通知为已读
     */
    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification != null && notification.getReceiverId().equals(userId)) {
            notification.setIsRead(true);
            notificationMapper.updateById(notification);
        }
    }

    /**
     * 标记所有通知为已读
     */
    public void markAllAsRead(Long userId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getReceiverId, userId)
               .eq(Notification::getIsRead, false);

        List<Notification> notifications = notificationMapper.selectList(wrapper);
        for (Notification notification : notifications) {
            notification.setIsRead(true);
            notificationMapper.updateById(notification);
        }
    }

    /**
     * 删除通知
     */
    public void deleteNotification(Long userId, Long notificationId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification != null && notification.getReceiverId().equals(userId)) {
            notificationMapper.deleteById(notificationId);
        }
    }

    /**
     * 实体转 VO
     */
    private NotificationVO convertToVO(Notification notification) {
        NotificationVO vo = new NotificationVO();
        BeanUtils.copyProperties(notification, vo);

        // 根据类型设置扩展信息
        if ("COMMENT".equals(notification.getType()) && notification.getReferenceId() != null) {
            Comment comment = commentMapper.selectById(notification.getReferenceId());
            if (comment != null) {
                Document document = documentMapper.selectById(comment.getDocumentId());
                if (document != null) {
                    vo.setDocumentTitle(document.getTitle());
                }
            }
        }

        return vo;
    }
}
