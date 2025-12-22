package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 添加/更新协作者请求DTO
 */
@Data
public class AddCollaboratorRequest {
    
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    @NotBlank(message = "角色不能为空")
    private String role; // EDITOR / VIEWER
}
