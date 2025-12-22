package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
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
import com.example.demo.dto.UpdateProfileRequest;
import com.example.demo.dto.UserVO;
import com.example.demo.entity.User;
import com.example.demo.mapper.PasswordResetTokenMapper;
import com.example.demo.mapper.UserMapper;
import com.example.demo.security.JwtUtil;

/**
 * 认证服务
 */
@Service
public class AuthService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordResetTokenMapper passwordResetTokenMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private RedisPasswordResetService redisPasswordResetService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 发送注册验证码
     */
    public void sendVerificationCode(String email) {
        // 检查邮箱是否已注册
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, email);
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }

        // 生成6位验证码
        String code = String.format("%06d", new Random().nextInt(1000000));
        
        // 存入Redis，5分钟有效
        String key = "reg_code:" + email;
        redisTemplate.opsForValue().set(key, code, 5, TimeUnit.MINUTES);
        
        // 发送邮件
        emailService.sendVerificationCodeEmail(email, code);
    }

    /**
     * 用户注册
     */
    public UserVO register(RegisterRequest request) {
        // 校验验证码
        String key = "reg_code:" + request.getEmail();
        String code = redisTemplate.opsForValue().get(key);
        if (code == null || !code.equals(request.getVerificationCode())) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_ERROR);
        }
        
        // 删除验证码
        redisTemplate.delete(key);

        // 检查邮箱是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, request.getEmail());
        User existingUser = userMapper.selectOne(wrapper);
        
        if (existingUser != null) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }

        // 创建新用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus("active");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        userMapper.insert(user);

        // 返回用户信息（不包含密码）
        return convertToVO(user);
    }

    /**
     * 用户登录
     */
    public LoginResponse login(LoginRequest request) {
        // 查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, request.getEmail());
        User user = userMapper.selectOne(wrapper);

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

        // 生成 Token
        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        // 返回 Token 和用户信息
        return new LoginResponse(token, convertToVO(user));
    }

    /**
     * 获取当前用户信息
     */
    public UserVO getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return convertToVO(user);
    }

    /**
     * 忘记密码 - 生成重置令牌并发送邮件
     * 使用Redis存储令牌，自动过期
     */
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        // 查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, request.getEmail());
        User user = userMapper.selectOne(wrapper);
        
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "该邮箱未注册");
        }

        // 生成重置令牌
        String token = UUID.randomUUID().toString().replace("-", "");
        
        // 保存令牌到Redis（有效期1小时，自动过期）
        redisPasswordResetService.saveToken(token, user.getId());
        
        // 发送邮件
        try {
            emailService.sendPasswordResetEmail(user.getEmail(), token);
        } catch (Exception e) {
            // 邮件发送失败，仍打印到控制台作为备用
            System.out.println("=".repeat(60));
            System.out.println("邮件发送失败，密码重置链接：http://localhost:3000/auth/reset-password?token=" + token);
            System.out.println("=".repeat(60));
        }
    }

    /**
     * 重置密码
     * 使用Redis验证令牌
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // 从Redis获取令牌对应的用户ID
        Long userId = redisPasswordResetService.getUserIdByToken(request.getToken());
        
        if (userId == null) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "重置令牌无效或已过期");
        }
        
        // 更新用户密码
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        
        // 删除已使用的令牌
        redisPasswordResetService.deleteToken(request.getToken());
    }

    /**
     * 更新个人资料
     */
    public UserVO updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        // 更新字段
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
        
        return convertToVO(user);
    }

    /**
     * 更新头像
     */
    public UserVO updateAvatar(Long userId, String avatarUrl) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        user.setAvatarUrl(avatarUrl);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        
        return convertToVO(user);
    }

    /**
     * 实体转 VO
     */
    private UserVO convertToVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
