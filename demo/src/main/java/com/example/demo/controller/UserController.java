package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.common.Result;
import com.example.demo.dto.UpdateProfileRequest;
import com.example.demo.dto.UserVO;
import com.example.demo.service.AuthService;
import com.example.demo.util.FileUploadUtil;

/**
 * 用户相关控制器
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private AuthService authService;

    @Autowired
    private FileUploadUtil fileUploadUtil;

    /**
     * 更新个人资料
     * PUT /api/users/me
     */
    @PutMapping("/me")
    public Result<UserVO> updateProfile(
            Authentication authentication,
            @Validated @RequestBody UpdateProfileRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        UserVO user = authService.updateProfile(userId, request);
        return Result.success("资料更新成功", user);
    }

    /**
     * 上传头像
     * POST /api/users/me/avatar
     */
    @PostMapping("/me/avatar")
    public Result<UserVO> uploadAvatar(
            Authentication authentication,
            @RequestParam("file") MultipartFile file) {
        Long userId = (Long) authentication.getPrincipal();
        
        // 上传文件并获取URL
        String avatarUrl = fileUploadUtil.uploadAvatar(file);
        
        // 更新用户头像
        UserVO user = authService.updateAvatar(userId, avatarUrl);
        
        return Result.success("头像上传成功", user);
    }
}
