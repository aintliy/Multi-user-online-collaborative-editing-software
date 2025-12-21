package com.example.demo.util;

import com.example.demo.common.BusinessException;
import com.example.demo.common.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 基于Redis的访问频率限制工具
 */
@Component
public class RateLimiter {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    /**
     * 检查并记录访问
     * 
     * @param key 限制的键（如：用户ID、IP地址等）
     * @param maxAttempts 最大尝试次数
     * @param timeWindow 时间窗口（秒）
     * @throws BusinessException 如果超过访问限制
     */
    public void checkRateLimit(String key, int maxAttempts, int timeWindow) {
        String redisKey = "rate_limit:" + key;
        
        // 获取当前计数
        String countStr = redisTemplate.opsForValue().get(redisKey);
        int count = countStr != null ? Integer.parseInt(countStr) : 0;
        
        if (count >= maxAttempts) {
            // 获取剩余过期时间
            Long ttl = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
            String message = String.format("访问过于频繁，请在 %d 秒后重试", ttl != null ? ttl : timeWindow);
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, message);
        }
        
        // 增加计数
        redisTemplate.opsForValue().increment(redisKey);
        
        // 如果是第一次访问，设置过期时间
        if (count == 0) {
            redisTemplate.expire(redisKey, timeWindow, TimeUnit.SECONDS);
        }
    }
    
    /**
     * 清除访问记录（用于成功操作后重置）
     */
    public void clearRateLimit(String key) {
        String redisKey = "rate_limit:" + key;
        redisTemplate.delete(redisKey);
    }
    
    /**
     * 获取剩余尝试次数
     */
    public int getRemainingAttempts(String key, int maxAttempts) {
        String redisKey = "rate_limit:" + key;
        String countStr = redisTemplate.opsForValue().get(redisKey);
        int count = countStr != null ? Integer.parseInt(countStr) : 0;
        return Math.max(0, maxAttempts - count);
    }
}
