package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.common.BusinessException;
import com.example.demo.common.ErrorCode;
import com.example.demo.dto.UserVO;
import com.example.demo.entity.User;
import com.example.demo.entity.UserFriend;
import com.example.demo.mapper.UserFriendMapper;
import com.example.demo.mapper.UserMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 好友服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FriendService {
    
    private final UserFriendMapper friendMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final OperationLogService operationLogService;
    
    /**
     * 发送好友请求
     */
    @Transactional(rollbackFor = Exception.class)
    public void sendFriendRequest(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new BusinessException(ErrorCode.CANNOT_ADD_SELF_AS_FRIEND);
        }
        
        User friend = userMapper.selectById(friendId);
        if (friend == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        // 检查是否已经是好友
        if (areFriends(userId, friendId)) {
            throw new BusinessException(ErrorCode.ALREADY_FRIENDS);
        }
        
        // 检查是否有待处理的请求
        Long count = friendMapper.selectCount(
            new LambdaQueryWrapper<UserFriend>()
                .eq(UserFriend::getUserId, userId)
                .eq(UserFriend::getFriendId, friendId)
                .eq(UserFriend::getStatus, "pending")
        );
        
        if (count > 0) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_ALREADY_SENT);
        }
        
        // 创建好友请求
        UserFriend friendRequest = new UserFriend();
        friendRequest.setUserId(userId);
        friendRequest.setFriendId(friendId);
        friendRequest.setStatus("pending");
        friendRequest.setCreatedAt(LocalDateTime.now());
        
        friendMapper.insert(friendRequest);
        
        // 发送通知
        User sender = userMapper.selectById(userId);
        notificationService.createNotification(
            friendId,
            "FRIEND_REQUEST",
            sender.getUsername() + " 向你发送了好友请求",
            friendRequest.getId()
        );
        
        // 记录操作日志
        operationLogService.log(userId, "SEND_FRIEND_REQUEST", "USER", friendId, 
            "发送好友请求给: " + friend.getUsername());
        
        log.info("发送好友请求: userId={}, friendId={}", userId, friendId);
    }
    
    /**
     * 接受好友请求
     */
    @Transactional(rollbackFor = Exception.class)
    public void acceptFriendRequest(Long userId, Long requestId) {
        UserFriend friendRequest = friendMapper.selectById(requestId);
        if (friendRequest == null) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_NOT_FOUND);
        }
        
        // 检查权限
        if (!friendRequest.getFriendId().equals(userId)) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_NO_PERMISSION);
        }
        
        if (!"pending".equals(friendRequest.getStatus())) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_ALREADY_PROCESSED);
        }
        
        // 更新请求状态
        friendRequest.setStatus("accepted");
        friendRequest.setUpdatedAt(LocalDateTime.now());
        friendMapper.updateById(friendRequest);
        
        // 创建反向好友关系
        UserFriend reverseFriend = new UserFriend();
        reverseFriend.setUserId(friendRequest.getFriendId());
        reverseFriend.setFriendId(friendRequest.getUserId());
        reverseFriend.setStatus("accepted");
        reverseFriend.setCreatedAt(LocalDateTime.now());
        reverseFriend.setUpdatedAt(LocalDateTime.now());
        friendMapper.insert(reverseFriend);
        
        // 发送通知
        User accepter = userMapper.selectById(userId);
        notificationService.createNotification(
            friendRequest.getUserId(),
            "FRIEND_REQUEST_ACCEPTED",
            accepter.getUsername() + " 接受了你的好友请求",
            requestId
        );
        
        // 记录操作日志
        operationLogService.log(userId, "ACCEPT_FRIEND_REQUEST", "USER", friendRequest.getUserId(), 
            "接受好友请求");
        
        log.info("接受好友请求: requestId={}, userId={}", requestId, userId);
    }
    
    /**
     * 拒绝好友请求
     */
    @Transactional(rollbackFor = Exception.class)
    public void rejectFriendRequest(Long userId, Long requestId) {
        UserFriend friendRequest = friendMapper.selectById(requestId);
        if (friendRequest == null) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_NOT_FOUND);
        }
        
        // 检查权限
        if (!friendRequest.getFriendId().equals(userId)) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_NO_PERMISSION);
        }
        
        if (!"pending".equals(friendRequest.getStatus())) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_ALREADY_PROCESSED);
        }
        
        // 更新请求状态
        friendRequest.setStatus("rejected");
        friendRequest.setUpdatedAt(LocalDateTime.now());
        friendMapper.updateById(friendRequest);
        
        // 记录操作日志
        operationLogService.log(userId, "REJECT_FRIEND_REQUEST", "USER", friendRequest.getUserId(), 
            "拒绝好友请求");
        
        log.info("拒绝好友请求: requestId={}, userId={}", requestId, userId);
    }
    
    /**
     * 删除好友
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeFriend(Long userId, Long friendId) {
        // 删除双向好友关系
        friendMapper.delete(
            new LambdaQueryWrapper<UserFriend>()
                .eq(UserFriend::getUserId, userId)
                .eq(UserFriend::getFriendId, friendId)
                .eq(UserFriend::getStatus, "accepted")
        );
        
        friendMapper.delete(
            new LambdaQueryWrapper<UserFriend>()
                .eq(UserFriend::getUserId, friendId)
                .eq(UserFriend::getFriendId, userId)
                .eq(UserFriend::getStatus, "accepted")
        );
        
        // 记录操作日志
        operationLogService.log(userId, "REMOVE_FRIEND", "USER", friendId, 
            "删除好友");
        
        log.info("删除好友: userId={}, friendId={}", userId, friendId);
    }
    
    /**
     * 获取好友列表
     */
    public List<UserVO> getFriends(Long userId) {
        List<UserFriend> friendships = friendMapper.selectList(
            new LambdaQueryWrapper<UserFriend>()
                .eq(UserFriend::getUserId, userId)
                .eq(UserFriend::getStatus, "accepted")
        );
        
        List<Long> friendIds = friendships.stream()
            .map(UserFriend::getFriendId)
            .collect(Collectors.toList());
        
        if (friendIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<User> friends = userMapper.selectBatchIds(friendIds);
        
        return friends.stream()
            .map(this::convertToUserVO)
            .collect(Collectors.toList());
    }
    
    /**
     * 获取待处理的好友请求
     */
    public List<UserVO> getPendingRequests(Long userId) {
        List<UserFriend> requests = friendMapper.selectList(
            new LambdaQueryWrapper<UserFriend>()
                .eq(UserFriend::getFriendId, userId)
                .eq(UserFriend::getStatus, "pending")
        );
        
        List<Long> senderIds = requests.stream()
            .map(UserFriend::getUserId)
            .collect(Collectors.toList());
        
        if (senderIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<User> senders = userMapper.selectBatchIds(senderIds);
        
        return senders.stream()
            .map(this::convertToUserVO)
            .collect(Collectors.toList());
    }
    
    /**
     * 检查是否是好友
     */
    private boolean areFriends(Long userId, Long friendId) {
        Long count = friendMapper.selectCount(
            new LambdaQueryWrapper<UserFriend>()
                .eq(UserFriend::getUserId, userId)
                .eq(UserFriend::getFriendId, friendId)
                .eq(UserFriend::getStatus, "accepted")
        );
        return count > 0;
    }
    
    /**
     * 转换为UserVO
     */
    private UserVO convertToUserVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setEmail(user.getEmail());
        vo.setPublicId(user.getPublicId());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setRole(user.getRole());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setUpdatedAt(user.getUpdatedAt());
        return vo;
    }
}
