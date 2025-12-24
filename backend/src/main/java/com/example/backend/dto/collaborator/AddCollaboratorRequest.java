package com.example.backend.dto.collaborator;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 添加协作者请求DTO
 */
@Data
public class AddCollaboratorRequest {
    
    @NotNull(message = "用户ID不能为空")
    private Long userId;
}
