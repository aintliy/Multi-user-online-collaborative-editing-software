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

import com.example.demo.common.Result;
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
    public Result<UserVO> getUserById(@PathVariable Long id) {
        UserVO user = userService.getUserById(id);
        return Result.success(user);
    }
    
    /**
     * 根据PublicID获取用户信息
     */
    @GetMapping("/public/{publicId}")
    public Result<UserVO> getUserByPublicId(@PathVariable String publicId) {
        UserVO user = userService.getUserByPublicId(publicId);
        return Result.success(user);
    }
    
    /**
     * 搜索用户
     */
    @GetMapping("/search")
    public Result<List<UserVO>> searchUsers(
            @RequestParam String keyword,
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        List<UserVO> users = userService.searchUsers(keyword, userId, limit);
        return Result.success(users);
    }
    
    /**
     * 上传头像
     */
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(
            @AuthenticationPrincipal Long userId,
            @RequestParam("file") MultipartFile file) throws IOException {
        String avatarUrl = userService.uploadAvatar(userId, file);
        return Result.success(avatarUrl);
    }
    
    // ============ 好友管理 ============
    
    /**
     * 发送好友请求
     */
    @PostMapping("/friends/{friendId}")
    public Result<Void> sendFriendRequest(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long friendId) {
        friendService.sendFriendRequest(userId, friendId);
        return Result.success();
    }
    
    /**
     * 接受好友请求
     */
    @PutMapping("/friends/requests/{requestId}/accept")
    public Result<Void> acceptFriendRequest(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long requestId) {
        friendService.acceptFriendRequest(userId, requestId);
        return Result.success();
    }
    
    /**
     * 拒绝好友请求
     */
    @PutMapping("/friends/requests/{requestId}/reject")
    public Result<Void> rejectFriendRequest(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long requestId) {
        friendService.rejectFriendRequest(userId, requestId);
        return Result.success();
    }
    
    /**
     * 删除好友
     */
    @DeleteMapping("/friends/{friendId}")
    public Result<Void> removeFriend(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long friendId) {
        friendService.removeFriend(userId, friendId);
        return Result.success();
    }
    
    /**
     * 获取好友列表
     */
    @GetMapping("/friends")
    public Result<List<UserVO>> getFriends(@AuthenticationPrincipal Long userId) {
        List<UserVO> friends = friendService.getFriends(userId);
        return Result.success(friends);
    }
    
    /**
     * 获取待处理的好友请求
     */
    @GetMapping("/friends/requests")
    public Result<List<UserVO>> getPendingRequests(@AuthenticationPrincipal Long userId) {
        List<UserVO> requests = friendService.getPendingRequests(userId);
        return Result.success(requests);
    }
}
