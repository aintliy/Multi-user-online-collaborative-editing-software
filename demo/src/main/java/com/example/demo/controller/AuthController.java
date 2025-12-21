package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.common.Result;
import com.example.demo.dto.ForgotPasswordRequest;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.dto.ResetPasswordRequest;
import com.example.demo.dto.UpdateProfileRequest;
import com.example.demo.dto.UserVO;
import com.example.demo.service.AuthService;
import com.example.demo.util.RateLimiter;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private RateLimiter rateLimiter;

    /**
     * 用户注册
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public Result<UserVO> register(@Validated @RequestBody RegisterRequest request) {
        UserVO user = authService.register(request);
        return Result.success("注册成功", user);
    }

    /**
     * 用户登录
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Validated @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return Result.success("登录成功", response);
    }

    /**
     * 获取当前用户信息
     * GET /api/auth/me
     */
    @GetMapping("/me")
    public Result<UserVO> getCurrentUser(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        UserVO user = authService.getCurrentUser(userId);
        return Result.success(user);
    }

    /**
     * 用户登出
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        // JWT 无状态，登出由前端处理（删除本地 Token）
        return Result.success("登出成功", null);
    }

    /**
     * 忘记密码 - 发送重置邮件
     * POST /api/auth/forgot-password
     * 限制：同一邮箱每5分钟最多3次请求
     */
    @PostMapping("/forgot-password")
    public Result<Void> forgotPassword(
            @Validated @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {
        
        // 频率限制：同一邮箱5分钟内最多3次
        String rateLimitKey = "forgot_pwd:" + request.getEmail();
        rateLimiter.checkRateLimit(rateLimitKey, 3, 300);
        
        authService.forgotPassword(request);
        return Result.success("密码重置邮件已发送，请查收", null);
    }

    /**
     * 重置密码
     * POST /api/auth/reset-password
     */
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Validated @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return Result.success("密码重置成功", null);
    }

    /**
     * 更新个人资料
     * PUT /api/auth/profile
     */
    @PutMapping("/profile")
    public Result<UserVO> updateProfile(
            Authentication authentication,
            @Validated @RequestBody UpdateProfileRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        UserVO user = authService.updateProfile(userId, request);
        return Result.success("资料更新成功", user);
    }
}
