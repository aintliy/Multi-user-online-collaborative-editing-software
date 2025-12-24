package com.example.backend.util;

import java.util.UUID;

/**
 * ID生成工具类
 */
public class IdGenerator {
    
    /**
     * 生成随机的公开用户ID
     */
    public static String generatePublicId() {
        return "u_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
    
    /**
     * 生成随机令牌
     */
    public static String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * 生成验证码
     */
    public static String generateVerificationCode() {
        return String.valueOf((int) ((Math.random() * 9 + 1) * 100000));
    }
}
