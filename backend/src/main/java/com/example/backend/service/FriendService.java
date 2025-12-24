package com.example.backend.service;

import com.example.backend.dto.friend.FriendDTO;
import com.example.backend.dto.friend.SendFriendRequest;
import com.example.backend.entity.User;
import com.example.backend.entity.UserFriend;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.UserFriendRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 好友服务
 */
@Service
@RequiredArgsConstructor
public class FriendService {
    
    private final UserFriendRepository friendRepository;
    private final UserService userService;
    private final NotificationService notificationService;
    
    /**
     * 发送好友请求
     */
    @Transactional
    public void sendFriendRequest(Long userId, SendFriendRequest request) {
        // 不能添加自己
        if (userId.equals(request.getTargetUserId())) {
            throw new BusinessException(ErrorCode.CANNOT_ADD_SELF, "不能添加自己为好友");
        }
        
        // 检查是否已经是好友
        if (friendRepository.areFriends(userId, request.getTargetUserId())) {
            throw new BusinessException(ErrorCode.ALREADY_FRIENDS, "你们已经是好友了");
        }
        
        // 检查是否已有待处理的请求
        if (friendRepository.existsByUserIdAndFriendId(userId, request.getTargetUserId())) {
            UserFriend existing = friendRepository.findByUserIdAndFriendId(userId, request.getTargetUserId()).orElse(null);
            if (existing != null && "PENDING".equals(existing.getStatus())) {
                throw new BusinessException(ErrorCode.FRIEND_REQUEST_EXISTS, "好友请求已发送，请等待对方处理");
            }
        }
        
        User user = userService.getUserById(userId);
        User targetUser = userService.getUserById(request.getTargetUserId());
        
        UserFriend friendRequest = UserFriend.builder()
                .user(user)
                .friend(targetUser)
                .status("PENDING")
                .build();
        
        friendRepository.save(friendRequest);
        
        // 发送通知
        String message = user.getUsername() + " 请求添加您为好友";
        if (request.getMessage() != null && !request.getMessage().isEmpty()) {
            message += "：" + request.getMessage();
        }
        notificationService.createNotification(
                request.getTargetUserId(),
                "FRIEND_REQUEST",
                friendRequest.getId(),
                message
        );
    }
    
    /**
     * 获取待处理的好友请求
     */
    public List<FriendDTO> getPendingRequests(Long userId) {
        List<UserFriend> requests = friendRepository.findPendingRequestsForUser(userId);
        return requests.stream()
                .map(r -> FriendDTO.fromEntity(r, false))
                .collect(Collectors.toList());
    }
    
    /**
     * 接受好友请求
     */
    @Transactional
    public void acceptFriendRequest(Long requestId, Long userId) {
        UserFriend request = friendRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_REQUEST_NOT_FOUND, "好友请求不存在"));
        
        // 验证是否是发给当前用户的请求
        if (!request.getFriend().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权处理此请求");
        }
        
        if (!"PENDING".equals(request.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该请求已被处理");
        }
        
        request.setStatus("ACCEPTED");
        friendRepository.save(request);
        
        // 通知请求发起人
        notificationService.createNotification(
                request.getUser().getId(),
                "FRIEND_REQUEST_ACCEPTED",
                requestId,
                request.getFriend().getUsername() + " 接受了您的好友请求"
        );
    }
    
    /**
     * 拒绝好友请求
     */
    @Transactional
    public void rejectFriendRequest(Long requestId, Long userId) {
        UserFriend request = friendRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_REQUEST_NOT_FOUND, "好友请求不存在"));
        
        if (!request.getFriend().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权处理此请求");
        }
        
        if (!"PENDING".equals(request.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该请求已被处理");
        }
        
        request.setStatus("REJECTED");
        friendRepository.save(request);
    }
    
    /**
     * 获取好友列表
     */
    public List<FriendDTO> getFriends(Long userId) {
        List<UserFriend> friendships = friendRepository.findAcceptedFriendsByUserId(userId);
        List<FriendDTO> friends = new ArrayList<>();
        
        for (UserFriend friendship : friendships) {
            // 判断当前用户是发起方还是接收方
            boolean isSender = friendship.getUser().getId().equals(userId);
            friends.add(FriendDTO.fromEntity(friendship, isSender));
        }
        
        return friends;
    }
    
    /**
     * 删除好友
     */
    @Transactional
    public void deleteFriend(Long friendUserId, Long userId) {
        // 查找双方的好友关系
        UserFriend friendship = friendRepository.findByUserIdAndFriendId(userId, friendUserId).orElse(null);
        if (friendship == null) {
            friendship = friendRepository.findByUserIdAndFriendId(friendUserId, userId).orElse(null);
        }
        
        if (friendship == null || !"ACCEPTED".equals(friendship.getStatus())) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_NOT_FOUND, "好友关系不存在");
        }
        
        friendRepository.delete(friendship);
    }
    
    /**
     * 检查是否为好友
     */
    public boolean areFriends(Long userId1, Long userId2) {
        return friendRepository.areFriends(userId1, userId2);
    }
}
