package com.example.backend.controller;

import com.example.backend.dto.ApiResponse;
import com.example.backend.dto.friend.FriendDTO;
import com.example.backend.dto.friend.SendFriendRequest;
import com.example.backend.entity.User;
import com.example.backend.service.FriendService;
import com.example.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 好友控制器
 */
@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {
    
    private final FriendService friendService;
    private final UserService userService;
    
    /**
     * 发送好友请求
     */
    @PostMapping("/requests")
    public ApiResponse<Void> sendFriendRequest(@AuthenticationPrincipal UserDetails userDetails,
                                                @Valid @RequestBody SendFriendRequest request) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        friendService.sendFriendRequest(user.getId(), request);
        return ApiResponse.success("好友请求已发送");
    }
    
    /**
     * 获取待处理的好友请求
     */
    @GetMapping("/requests")
    public ApiResponse<List<FriendDTO>> getPendingRequests(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        List<FriendDTO> requests = friendService.getPendingRequests(user.getId());
        return ApiResponse.success(requests);
    }
    
    /**
     * 接受好友请求
     */
    @PostMapping("/requests/{requestId}/accept")
    public ApiResponse<Void> acceptFriendRequest(@AuthenticationPrincipal UserDetails userDetails,
                                                  @PathVariable Long requestId) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        friendService.acceptFriendRequest(requestId, user.getId());
        return ApiResponse.success("已接受好友请求");
    }
    
    /**
     * 拒绝好友请求
     */
    @PostMapping("/requests/{requestId}/reject")
    public ApiResponse<Void> rejectFriendRequest(@AuthenticationPrincipal UserDetails userDetails,
                                                  @PathVariable Long requestId) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        friendService.rejectFriendRequest(requestId, user.getId());
        return ApiResponse.success("已拒绝好友请求");
    }
    
    /**
     * 获取好友列表
     */
    @GetMapping
    public ApiResponse<List<FriendDTO>> getFriends(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        List<FriendDTO> friends = friendService.getFriends(user.getId());
        return ApiResponse.success(friends);
    }
    
    /**
     * 删除好友
     */
    @DeleteMapping("/{friendUserId}")
    public ApiResponse<Void> deleteFriend(@AuthenticationPrincipal UserDetails userDetails,
                                           @PathVariable Long friendUserId) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        friendService.deleteFriend(friendUserId, user.getId());
        return ApiResponse.success("已删除好友");
    }
}
