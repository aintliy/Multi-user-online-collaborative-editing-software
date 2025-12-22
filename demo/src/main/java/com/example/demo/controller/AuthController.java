package com.example.demo.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.dto.*;
import com.example.demo.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器（重构版）
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    
    /**
     * 发送验证码
     */
    @PostMapping("/send-verification-code")
    public ApiResponse<Void> sendVerificationCode(@Valid @RequestBody SendVerificationCodeRequest request) {
        authService.sendVerificationCode(request);
        return ApiResponse.success();
    }
    
    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ApiResponse<UserVO> register(@Valid @RequestBody RegisterRequest request) {
        UserVO user = authService.register(request);
        return ApiResponse.success(user);
    }
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ApiResponse.success(response);
    }
    
    /**
     * 忘记密码
     */
    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ApiResponse.success();
    }
    
    /**
     * 重置密码
     */
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.success();
    }
    
    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public ApiResponse<UserVO> getCurrentUser(@AuthenticationPrincipal Long userId) {
        UserVO user = authService.getCurrentUser(userId);
        return ApiResponse.success(user);
    }
    
    /**
     * 更新个人资料
     */
    @PutMapping("/profile")
    public ApiResponse<Void> updateProfile(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        authService.updateProfile(userId, request);
        return ApiResponse.success();
    }
    
    /**
     * 登出（前端删除token即可）
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.success();
    }
}
