package com.example.backend.dto.collaborator;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 通过邀请链接加入协作请求DTO
 */
@Data
public class JoinByInviteRequest {
    
    @NotBlank(message = "邀请令牌不能为空")
    private String token;
}
