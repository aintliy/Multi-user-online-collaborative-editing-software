package com.example.demo.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 密码重置令牌实体
 */
@Data
@TableName("password_reset_tokens")
public class PasswordResetToken {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 重置令牌
     */
    private String token;
    
    /**
     * 过期时间
     */
    private LocalDateTime expiresAt;
    
    /**
     * 是否已使用
     */
    private Boolean used;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
