package com.example.backend.service;

import com.example.backend.dto.auth.*;
import com.example.backend.entity.User;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.JwtUtil;
import com.example.backend.util.IdGenerator;
import com.example.backend.util.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * 认证服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final StringRedisTemplate stringRedisTemplate;
    private final RateLimiter rateLimiter;
    private final JavaMailSender mailSender;
    
    private static final String VERIFICATION_CODE_PREFIX = "reg_code:";
    private static final String PASSWORD_RESET_PREFIX = "pwd_reset:";
    private static final int VERIFICATION_CODE_EXPIRE = 300; // 5分钟
    private static final int PASSWORD_RESET_EXPIRE = 3600; // 1小时
    
    /**
     * 发送注册验证码
     */
    public void sendVerificationCode(String email) {
        // 检查邮箱是否已注册
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "该邮箱已被注册");
        }
        
        // 频率限制
        rateLimiter.checkRateLimit("send_code:" + email, 5, 300);
        
        // 生成验证码
        String code = IdGenerator.generateVerificationCode();
        
        // 存储到Redis
        stringRedisTemplate.opsForValue().set(
                VERIFICATION_CODE_PREFIX + email, 
                code, 
                VERIFICATION_CODE_EXPIRE, 
                TimeUnit.SECONDS
        );
        
        // 发送邮件
        try {
            sendEmail(email, "注册验证码", "您的注册验证码是：" + code + "，有效期5分钟。");
        } catch (Exception e) {
            log.error("发送邮件失败", e);
            // 开发环境下打印验证码
            log.info("验证码: {} -> {}", email, code);
        }
    }
    
    /**
     * 用户注册
     */
    @Transactional
    public UserDTO register(RegisterRequest request) {
        // 检查邮箱是否已注册
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "该邮箱已被注册");
        }
        
        // 验证验证码
        String storedCode = stringRedisTemplate.opsForValue().get(VERIFICATION_CODE_PREFIX + request.getEmail());
        if (storedCode == null || !storedCode.equals(request.getVerificationCode())) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_ERROR, "验证码错误或已过期");
        }
        
        // 生成唯一的公开ID
        String publicId;
        do {
            publicId = IdGenerator.generatePublicId();
        } while (userRepository.existsByPublicId(publicId));
        
        // 创建用户
        User user = User.builder()
                .publicId(publicId)
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role("USER")
                .status("active")
                .build();
        
        user = userRepository.save(user);
        
        // 删除验证码
        stringRedisTemplate.delete(VERIFICATION_CODE_PREFIX + request.getEmail());
        
        return UserDTO.fromEntity(user);
    }
    
    /**
     * 用户登录
     */
    public LoginResponse login(LoginRequest request) {
        // 认证
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        
        // 获取用户信息
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
        
        // 检查用户状态
        if (!"active".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.USER_DISABLED, "账号已被禁用，请联系管理员");
        }
        
        // 生成JWT
        String token = jwtUtil.generateToken(userDetails, user.getId());
        
        return LoginResponse.builder()
                .token(token)
                .user(UserDTO.fromEntity(user))
                .build();
    }
    
    /**
     * 获取当前用户信息
     */
    public UserDTO getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
        return UserDTO.fromEntity(user);
    }
    
    /**
     * 更新个人资料
     */
    @Transactional
    public UserDTO updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
        
        if (request.getUsername() != null) {
            user.setUsername(request.getUsername());
        }
        if (request.getProfile() != null) {
            user.setProfile(request.getProfile());
        }
        
        user = userRepository.save(user);
        return UserDTO.fromEntity(user);
    }
    
    /**
     * 申请密码重置
     */
    public void forgotPassword(String email) {
        // 频率限制：5分钟内最多3次
        rateLimiter.checkRateLimit("forgot_pwd:" + email, 3, 300);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
        
        // 生成重置令牌
        String token = IdGenerator.generateToken();
        
        // 存储到Redis
        stringRedisTemplate.opsForValue().set(
                PASSWORD_RESET_PREFIX + token, 
                user.getId().toString(), 
                PASSWORD_RESET_EXPIRE, 
                TimeUnit.SECONDS
        );
        
        // 发送重置邮件
        String resetLink = "http://localhost:3000/reset-password?token=" + token;
        try {
            sendEmail(email, "密码重置", "请点击以下链接重置密码（1小时内有效）：\n" + resetLink);
        } catch (Exception e) {
            log.error("发送邮件失败", e);
            log.info("密码重置链接: {}", resetLink);
        }
    }
    
    /**
     * 重置密码
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // 从Redis获取用户ID
        String userIdStr = stringRedisTemplate.opsForValue().get(PASSWORD_RESET_PREFIX + request.getToken());
        if (userIdStr == null) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "令牌无效或已过期");
        }
        
        Long userId = Long.parseLong(userIdStr);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
        
        // 更新密码
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        
        // 删除令牌
        stringRedisTemplate.delete(PASSWORD_RESET_PREFIX + request.getToken());
    }
    
    /**
     * 更新头像
     */
    @Transactional
    public String updateAvatar(String email, String avatarUrl) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
        
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);
        return avatarUrl;
    }
    
    private void sendEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
}
