package com.example.demo.dto;

import lombok.Data;

/**
 * 登录响应 DTO
 */
@Data
public class LoginResponse {

    private String token;

    private UserVO user;

    public LoginResponse(String token, UserVO user) {
        this.token = token;
        this.user = user;
    }
}
