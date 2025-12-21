package com.example.demo.dto;

import lombok.Data;

/**
 * 更新个人资料请求
 */
@Data
public class UpdateProfileRequest {
    
    private String username;
    
    private String phone;
    
    private String profile;
}
