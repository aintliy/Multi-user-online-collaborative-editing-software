package com.example.backend.util;

import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 频率限制工具类
 */
@Component
@RequiredArgsConstructor
public class RateLimiter {
    
    private final StringRedisTemplate stringRedisTemplate;
    
    /**
     * 检查频率限制
     * @param key 限制键
     * @param maxAttempts 最大尝试次数
     * @param windowSeconds 时间窗口（秒）
     */
    public void checkRateLimit(String key, int maxAttempts, int windowSeconds) {
        String redisKey = "rate_limit:" + key;
        String countStr = stringRedisTemplate.opsForValue().get(redisKey);
        
        if (countStr != null) {
            int count = Integer.parseInt(countStr);
            if (count >= maxAttempts) {
                Long ttl = stringRedisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
                throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, 
                        "访问过于频繁，请在 " + (ttl != null ? ttl : windowSeconds) + " 秒后重试");
            }
            stringRedisTemplate.opsForValue().increment(redisKey);
        } else {
            stringRedisTemplate.opsForValue().set(redisKey, "1", windowSeconds, TimeUnit.SECONDS);
        }
    }
}
