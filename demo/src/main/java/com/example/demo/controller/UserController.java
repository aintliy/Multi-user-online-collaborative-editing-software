package com.example.demo.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.common.ApiResponse;
import com.example.demo.dto.UserVO;
import com.example.demo.service.FriendService;
import com.example.demo.service.UserService;

import lombok.RequiredArgsConstructor;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    private final FriendService friendService;
    
    /**
     * 根据ID获取用户信息
     */
    @GetMapping("/{id}")
    public ApiResponse<UserVO> getUserById(@PathVariable Long id) {
        UserVO user = userService.getUserById(id);
        return ApiResponse.success(user);
    }
    
    /**
     * 根据PublicID获取用户信息
     */
    @GetMapping("/public/{publicId}")
    public ApiResponse<UserVO> getUserByPublicId(@PathVariable String publicId) {
        UserVO user = userService.getUserByPublicId(publicId);
        return ApiResponse.success(user);
    }
    
    /**
     * 搜索用户
     */
    @GetMapping("/search")
    public ApiResponse<List<UserVO>> searchUsers(
            @RequestParam String keyword,
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        List<UserVO> users = userService.searchUsers(keyword, userId, limit);
        return ApiResponse.success(users);
    }
    
    /**
     * 上传头像
     */
    @PostMapping("/avatar")
    public ApiResponse<String> uploadAvatar(
            @AuthenticationPrincipal Long userId,
            @RequestParam("file") MultipartFile file) throws IOException {
        String avatarUrl = userService.uploadAvatar(userId, file);
        return ApiResponse.success(avatarUrl);
    }
    
    // ============ 好友管理 ============
    
    /**
     * 发送好友请求
     */
    @PostMapping("/friends/{friendId}")
    public ApiResponse<Void> sendFriendRequest(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long friendId) {
        friendService.sendFriendRequest(userId, friendId);
        return ApiResponse.success();
    }
    
    /**
     * 接受好友请求
     */
    @PutMapping("/friends/requests/{requestId}/accept")
    public ApiResponse<Void> acceptFriendRequest(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long requestId) {
        friendService.acceptFriendRequest(userId, requestId);
        return ApiResponse.success();
    }
    
    /**
     * 拒绝好友请求
     */
    @PutMapping("/friends/requests/{requestId}/reject")
    public ApiResponse<Void> rejectFriendRequest(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long requestId) {
        friendService.rejectFriendRequest(userId, requestId);
        return ApiResponse.success();
    }
    
    /**
     * 删除好友
     */
    @DeleteMapping("/friends/{friendId}")
    public ApiResponse<Void> removeFriend(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long friendId) {
        friendService.removeFriend(userId, friendId);
        return ApiResponse.success();
    }
    
    /**
     * 获取好友列表
     */
    @GetMapping("/friends")
    public ApiResponse<List<UserVO>> getFriends(@AuthenticationPrincipal Long userId) {
        List<UserVO> friends = friendService.getFriends(userId);
        return ApiResponse.success(friends);
    }
    
    /**
     * 获取待处理的好友请求
     */
    @GetMapping("/friends/requests")
    public ApiResponse<List<UserVO>> getPendingRequests(@AuthenticationPrincipal Long userId) {
        List<UserVO> requests = friendService.getPendingRequests(userId);
        return ApiResponse.success(requests);
    }
}
