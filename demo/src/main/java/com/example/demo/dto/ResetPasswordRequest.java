package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 重置密码请求DTO
 */
@Data
public class ResetPasswordRequest {
    
    @NotBlank(message = "令牌不能为空")
    private String token;
    
    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}
