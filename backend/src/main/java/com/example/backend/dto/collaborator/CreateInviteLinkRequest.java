package com.example.backend.dto.collaborator;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 创建邀请链接请求DTO
 */
@Data
public class CreateInviteLinkRequest {
    
    private Integer maxUses;
    private LocalDateTime expiresAt;
}
