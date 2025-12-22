package com.example.demo.util;

import java.util.UUID;

/**
 * PublicId生成工具类
 */
public class PublicIdGenerator {
    
    /**
     * 生成用户的公开ID
     * 格式：u_随机字符串
     */
    public static String generateUserPublicId() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return "u_" + uuid.substring(0, 8);
    }
}
