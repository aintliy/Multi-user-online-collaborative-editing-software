package com.example.backend.controller;

import com.example.backend.dto.ApiResponse;
import com.example.backend.dto.auth.UserDTO;
import com.example.backend.entity.User;
import com.example.backend.service.AuthService;
import com.example.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    private final AuthService authService;
    
    @Value("${file.avatar-dir:./uploads/avatars}")
    private String avatarDir;
    
    /**
     * 搜索用户
     */
    @GetMapping("/search")
    public ApiResponse<List<UserDTO>> searchUsers(@RequestParam String keyword) {
        List<UserDTO> users = userService.searchUsers(keyword);
        return ApiResponse.success(users);
    }
    
    /**
     * 上传头像
     */
    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, String>> uploadAvatar(@AuthenticationPrincipal UserDetails userDetails,
                                                          @RequestParam("file") MultipartFile file) throws IOException {
        // 验证文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ApiResponse.error(1001, "只支持图片文件");
        }
        
        // 创建上传目录
        Path uploadPath = Paths.get(avatarDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // 生成文件名
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null ? 
                originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
        String filename = UUID.randomUUID().toString() + extension;
        
        // 保存文件
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath);
        
        // 更新用户头像URL
        String avatarUrl = "/uploads/avatars/" + filename;
        authService.updateAvatar(userDetails.getUsername(), avatarUrl);
        
        return ApiResponse.success("头像上传成功", Map.of("avatarUrl", avatarUrl));
    }
    
    /**
     * 查看用户公开仓库
     */
    @GetMapping("/{publicId}/repos")
    public ApiResponse<UserDTO> getUserRepos(@PathVariable String publicId) {
        User user = userService.getUserByPublicId(publicId);
        return ApiResponse.success(UserDTO.fromEntity(user));
    }
}
