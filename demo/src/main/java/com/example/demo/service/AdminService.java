package com.example.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.BusinessException;
import com.example.demo.common.ErrorCode;
import com.example.demo.dto.PageResponse;
import com.example.demo.dto.UserVO;
import com.example.demo.entity.User;
import com.example.demo.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理员服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {
    
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final OperationLogService operationLogService;
    
    /**
     * 获取用户列表
     */
    public PageResponse<UserVO> getUsers(Integer page, Integer pageSize, String keyword, String role, String status) {
        Page<User> pageObj = new Page<>(page, pageSize);
        
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        
        // 关键字搜索
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w
                .like(User::getUsername, keyword)
                .or()
                .like(User::getEmail, keyword)
                .or()
                .like(User::getPublicId, keyword)
            );
        }
        
        // 角色筛选
        if (role != null && !role.trim().isEmpty()) {
            wrapper.eq(User::getRole, role);
        }
        
        // 状态筛选
        if (status != null && !status.trim().isEmpty()) {
            wrapper.eq(User::getStatus, status);
        }
        
        wrapper.orderByDesc(User::getCreatedAt);
        
        IPage<User> result = userMapper.selectPage(pageObj, wrapper);
        
        List<UserVO> items = result.getRecords().stream()
            .map(this::convertToUserVO)
            .collect(Collectors.toList());
        
        return new PageResponse<>(items, page, pageSize, result.getTotal());
    }
    
    /**
     * 更新用户角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateUserRole(Long adminId, Long userId, String newRole) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        // 不能修改自己的角色
        if (userId.equals(adminId)) {
            throw new BusinessException(ErrorCode.CANNOT_UPDATE_SELF_ROLE);
        }
        
        String oldRole = user.getRole();
        user.setRole(newRole);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        
        // 记录操作日志
        operationLogService.log(adminId, "UPDATE_USER_ROLE", "USER", userId, 
            "更新用户角色: " + oldRole + " -> " + newRole);
        
        log.info("更新用户角色: adminId={}, userId={}, oldRole={}, newRole={}", 
            adminId, userId, oldRole, newRole);
    }
    
    /**
     * 更新用户状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long adminId, Long userId, String newStatus) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        // 不能修改自己的状态
        if (userId.equals(adminId)) {
            throw new BusinessException(ErrorCode.CANNOT_UPDATE_SELF_STATUS);
        }
        
        String oldStatus = user.getStatus();
        user.setStatus(newStatus);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        
        // 记录操作日志
        operationLogService.log(adminId, "UPDATE_USER_STATUS", "USER", userId, 
            "更新用户状态: " + oldStatus + " -> " + newStatus);
        
        log.info("更新用户状态: adminId={}, userId={}, oldStatus={}, newStatus={}", 
            adminId, userId, oldStatus, newStatus);
    }
    
    /**
     * 删除用户
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long adminId, Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        // 不能删除自己
        if (userId.equals(adminId)) {
            throw new BusinessException(ErrorCode.CANNOT_DELETE_SELF);
        }
        
        // 软删除
        user.setStatus("deleted");
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        
        // 记录操作日志
        operationLogService.log(adminId, "DELETE_USER", "USER", userId, 
            "删除用户: " + user.getUsername());
        
        log.info("删除用户: adminId={}, userId={}", adminId, userId);
    }
    
    /**
     * 重置用户密码
     */
    @Transactional(rollbackFor = Exception.class)
    public void resetUserPassword(Long adminId, Long userId, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        
        // 记录操作日志
        operationLogService.log(adminId, "RESET_USER_PASSWORD", "USER", userId, 
            "重置用户密码");
        
        log.info("重置用户密码: adminId={}, userId={}", adminId, userId);
    }
    
    /**
     * 获取系统统计
     */
    public SystemStatsVO getSystemStats() {
        // 统计用户数
        Long totalUsers = userMapper.selectCount(null);
        Long activeUsers = userMapper.selectCount(
            new LambdaQueryWrapper<User>()
                .eq(User::getStatus, "active")
        );
        Long adminUsers = userMapper.selectCount(
            new LambdaQueryWrapper<User>()
                .eq(User::getRole, "ADMIN")
        );
        
        SystemStatsVO stats = new SystemStatsVO();
        stats.setTotalUsers(totalUsers.intValue());
        stats.setActiveUsers(activeUsers.intValue());
        stats.setAdminUsers(adminUsers.intValue());
        
        return stats;
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
     * 系统统计VO
     */
    public static class SystemStatsVO {
        private Integer totalUsers;
        private Integer activeUsers;
        private Integer adminUsers;
        
        public Integer getTotalUsers() {
            return totalUsers;
        }
        
        public void setTotalUsers(Integer totalUsers) {
            this.totalUsers = totalUsers;
        }
        
        public Integer getActiveUsers() {
            return activeUsers;
        }
        
        public void setActiveUsers(Integer activeUsers) {
            this.activeUsers = activeUsers;
        }
        
        public Integer getAdminUsers() {
            return adminUsers;
        }
        
        public void setAdminUsers(Integer adminUsers) {
            this.adminUsers = adminUsers;
        }
    }
}
