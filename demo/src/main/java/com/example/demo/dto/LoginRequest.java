package com.example.demo.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 用户登录请求 DTO
 */
@Data
public class LoginRequest {

    @NotBlank(message = "邮箱不能为空")
    private String email;

    @NotBlank(message = "密码不能为空")
    private String password;
}
