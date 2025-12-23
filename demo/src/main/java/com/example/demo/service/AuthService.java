package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.common.BusinessException;
import com.example.demo.common.ErrorCode;
import com.example.demo.dto.ForgotPasswordRequest;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.dto.ResetPasswordRequest;
import com.example.demo.dto.SendVerificationCodeRequest;
import com.example.demo.dto.UpdateProfileRequest;
import com.example.demo.dto.UserVO;
import com.example.demo.entity.User;
import com.example.demo.mapper.UserMapper;
import com.example.demo.security.JwtUtil;
import com.example.demo.util.PublicIdGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 认证服务实现
 * 严格按照接口设计文档实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final JavaMailSender mailSender;
    
    /**
     * 发送注册验证码
     */
    public void sendVerificationCode(SendVerificationCodeRequest request) {
        String email = request.getEmail();
        
        // 检查邮箱是否已注册
        User existingUser = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getEmail, email)
        );
        if (existingUser != null) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }
        
        // 生成6位验证码
        String code = String.format("%06d", (int)(Math.random() * 1000000));
        
        // 存储到Redis，有效期5分钟
        String key = "reg_code:" + email;
        redisTemplate.opsForValue().set(key, code, 5, TimeUnit.MINUTES);
        
        // 发送邮件
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("【协作编辑系统】注册验证码");
            message.setText("您的注册验证码是：" + code + "\n\n验证码有效期5分钟，请尽快完成注册。");
            mailSender.send(message);
            
            log.info("发送验证码到邮箱: {}", email);
        } catch (Exception e) {
            log.error("发送邮件失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "发送验证码失败");
        }
    }
    
    /**
     * 用户注册
     */
    @Transactional(rollbackFor = Exception.class)
    public UserVO register(RegisterRequest request) {
        // 验证邮箱唯一性
        User existingUser = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getEmail, request.getEmail())
        );
        if (existingUser != null) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }
        
        // 验证验证码
        String key = "reg_code:" + request.getEmail();
        String storedCode = redisTemplate.opsForValue().get(key);
        if (storedCode == null || !storedCode.equals(request.getVerificationCode())) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_ERROR);
        }
        
        // 创建用户
        User user = new User();
        user.setPublicId(PublicIdGenerator.generateUserPublicId());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("USER"); // 注册接口只创建普通用户
        user.setStatus("active");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        userMapper.insert(user);
        
        // 删除验证码
        redisTemplate.delete(key);
        
        log.info("用户注册成功: {}, publicId: {}", user.getEmail(), user.getPublicId());
        
        return convertToUserVO(user);
    }
    
    /**
     * 用户登录
     */
    public LoginResponse login(LoginRequest request) {
        // 查询用户
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getEmail, request.getEmail())
        );
        
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        // 检查账号状态
        if ("disabled".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
        
        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.USER_PASSWORD_ERROR);
        }
        
        // 生成JWT（重构版：包含role）
        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole());
        
        // 构造用户VO
        UserVO userVO = convertToUserVO(user);
        
        log.info("用户登录成功: {}", user.getEmail());
        
        return new LoginResponse(token, userVO);
    }
    
    /**
     * 申请密码重置
     */
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail();
        
        // 频率限制：5分钟最多3次
        String rateLimitKey = "rate_limit:forgot_pwd:" + email;
        String count = redisTemplate.opsForValue().get(rateLimitKey);
        if (count != null && Integer.parseInt(count) >= 3) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
        }
        
        // 查询用户
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getEmail, email)
        );
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        // 生成重置令牌
        String token = UUID.randomUUID().toString();
        
        // 存储到Redis，有效期1小时
        String key = "pwd_reset:" + token;
        redisTemplate.opsForValue().set(key, user.getId().toString(), 1, TimeUnit.HOURS);
        
        // 记录频率限制
        if (count == null) {
            redisTemplate.opsForValue().set(rateLimitKey, "1", 5, TimeUnit.MINUTES);
        } else {
            redisTemplate.opsForValue().increment(rateLimitKey);
        }
        
        // 发送邮件
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("【协作编辑系统】密码重置");
            message.setText("您好，\n\n您申请了密码重置。请点击以下链接重置密码（1小时内有效）：\n\n" +
                    "http://localhost:3000/auth/reset-password?token=" + token + "\n\n" +
                    "如果这不是您的操作，请忽略此邮件。");
            mailSender.send(message);
            
            log.info("发送密码重置邮件到: {}", email);
        } catch (Exception e) {
            log.error("发送邮件失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "发送邮件失败");
        }
    }
    
    /**
     * 重置密码
     */
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(ResetPasswordRequest request) {
        String token = request.getToken();
        
        // 从Redis获取用户ID
        String key = "pwd_reset:" + token;
        String userIdStr = redisTemplate.opsForValue().get(key);
        
        if (userIdStr == null) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        
        Long userId = Long.parseLong(userIdStr);
        
        // 更新密码
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        
        // 删除令牌
        redisTemplate.delete(key);
        
        log.info("用户密码重置成功: {}", user.getEmail());
    }
    
    /**
     * 获取当前登录用户信息
     */
    public UserVO getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        return convertToUserVO(user);
    }
    
    /**
     * 更新个人资料
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        if (request.getUsername() != null) {
            user.setUsername(request.getUsername());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getProfile() != null) {
            user.setProfile(request.getProfile());
        }
        
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        
        log.info("用户资料更新: {}", userId);
    }
    
    /**
     * 转换User实体为UserVO
     */
    private UserVO convertToUserVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setPublicId(user.getPublicId());
        vo.setUsername(user.getUsername());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setProfile(user.getProfile());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setUpdatedAt(user.getUpdatedAt());
        return vo;
    }
}
