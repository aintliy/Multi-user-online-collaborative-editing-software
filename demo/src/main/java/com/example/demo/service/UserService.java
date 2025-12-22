package com.example.demo.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 用户服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserMapper userMapper;
    private final UserFriendMapper friendMapper;
    private final OperationLogService operationLogService;
    
    private static final String UPLOAD_DIR = "uploads/avatars/";
    
    /**
     * 根据ID获取用户信息
     */
    public UserVO getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        return convertToUserVO(user);
    }
    
    /**
     * 根据PublicID获取用户信息
     */
    public UserVO getUserByPublicId(String publicId) {
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getPublicId, publicId)
        );
        
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        return convertToUserVO(user);
    }
    
    /**
     * 搜索用户
     */
    public List<UserVO> searchUsers(String keyword, Long currentUserId, Integer limit) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
            .like(User::getUsername, keyword)
            .or()
            .like(User::getEmail, keyword)
            .or()
            .like(User::getPublicId, keyword)
        );
        wrapper.eq(User::getStatus, "active");
        wrapper.ne(User::getId, currentUserId); // 排除当前用户
        
        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + limit);
        }
        
        List<User> users = userMapper.selectList(wrapper);
        
        return users.stream()
            .map(this::convertToUserVO)
            .collect(Collectors.toList());
    }
    
    /**
     * 上传头像
     */
    @Transactional(rollbackFor = Exception.class)
    public String uploadAvatar(Long userId, MultipartFile file) throws IOException {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        // 验证文件
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_EMPTY);
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new BusinessException(ErrorCode.FILE_NAME_INVALID);
        }
        
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        if (!isValidImageExtension(extension)) {
            throw new BusinessException(ErrorCode.FILE_TYPE_INVALID);
        }
        
        // 生成文件名
        String filename = UUID.randomUUID().toString() + extension;
        
        // 确保上传目录存在
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // 保存文件
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath);
        
        // 更新用户头像URL
        String avatarUrl = "/uploads/avatars/" + filename;
        user.setAvatarUrl(avatarUrl);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        
        // 记录操作日志
        operationLogService.log(userId, "UPLOAD_AVATAR", "USER", userId, 
            "上传头像");
        
        log.info("上传头像: userId={}, filename={}", userId, filename);
        
        return avatarUrl;
    }
    
    /**
     * 检查用户是否有管理员权限
     */
    public boolean isAdmin(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null && "ADMIN".equals(user.getRole());
    }
    
    /**
     * 获取用户统计信息
     */
    public UserStatsVO getUserStats(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        // 统计好友数量
        Long friendCount = friendMapper.selectCount(
            new LambdaQueryWrapper<UserFriend>()
                .eq(UserFriend::getUserId, userId)
                .eq(UserFriend::getStatus, "accepted")
        );
        
        UserStatsVO stats = new UserStatsVO();
        stats.setUserId(userId);
        stats.setFriendCount(friendCount.intValue());
        
        return stats;
    }
    
    /**
     * 验证图片扩展名
     */
    private boolean isValidImageExtension(String extension) {
        String[] validExtensions = {".jpg", ".jpeg", ".png", ".gif", ".webp"};
        for (String validExt : validExtensions) {
            if (validExt.equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
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
        vo.setStatus(user.getStatus());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }
    
    /**
     * 用户统计VO
     */
    public static class UserStatsVO {
        private Long userId;
        private Integer friendCount;
        
        public Long getUserId() {
            return userId;
        }
        
        public void setUserId(Long userId) {
            this.userId = userId;
        }
        
        public Integer getFriendCount() {
            return friendCount;
        }
        
        public void setFriendCount(Integer friendCount) {
            this.friendCount = friendCount;
        }
    }
}
