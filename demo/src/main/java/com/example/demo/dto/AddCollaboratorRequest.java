package com.example.demo.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

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
