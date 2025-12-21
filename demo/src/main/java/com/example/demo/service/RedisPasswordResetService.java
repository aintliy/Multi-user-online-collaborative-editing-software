package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis密码重置令牌服务
 * 使用Redis存储密码重置令牌，自动过期
 */
@Service
public class RedisPasswordResetService {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    private static final String TOKEN_PREFIX = "pwd_reset:";
    private static final long TOKEN_EXPIRATION = 1; // 1小时
    
    /**
     * 保存密码重置令牌
     * 
     * @param token 令牌
     * @param userId 用户ID
     */
    public void saveToken(String token, Long userId) {
        String key = TOKEN_PREFIX + token;
        redisTemplate.opsForValue().set(key, String.valueOf(userId), TOKEN_EXPIRATION, TimeUnit.HOURS);
    }
    
    /**
     * 获取令牌对应的用户ID
     * 
     * @param token 令牌
     * @return 用户ID，如果令牌不存在或已过期则返回null
     */
    public Long getUserIdByToken(String token) {
        String key = TOKEN_PREFIX + token;
        String userIdStr = redisTemplate.opsForValue().get(key);
        return userIdStr != null ? Long.parseLong(userIdStr) : null;
    }
    
    /**
     * 删除令牌（使用后）
     * 
     * @param token 令牌
     */
    public void deleteToken(String token) {
        String key = TOKEN_PREFIX + token;
        redisTemplate.delete(key);
    }
    
    /**
     * 检查令牌是否存在
     * 
     * @param token 令牌
     * @return true-存在，false-不存在或已过期
     */
    public boolean tokenExists(String token) {
        String key = TOKEN_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
