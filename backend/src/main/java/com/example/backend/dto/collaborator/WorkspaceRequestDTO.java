package com.example.backend.dto.collaborator;

import java.time.LocalDateTime;


import com.example.backend.dto.auth.UserDTO;
import com.example.backend.dto.document.DocumentDTO;
import com.example.backend.entity.DocumentWorkspaceRequest;

import lombok.Data;

/**
 * 协作申请请求DTO
 */
@Data
public class WorkspaceRequestDTO {
    private Long id;
    

    private DocumentDTO document;
    
    /**
     * 申请人
     */
    private UserDTO applicant;
    
    /**
     * 申请状态: PENDING / APPROVED / REJECTED
     */
    private String status;
    
    /**
     * 申请理由
     */
    private String message;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime handledAt;
    
    /**
     * 处理人（通常为owner）
     */
    private UserDTO handledBy;

    public static WorkspaceRequestDTO fromEntity(DocumentWorkspaceRequest request) {
        WorkspaceRequestDTO dto = new WorkspaceRequestDTO();
        dto.setId(request.getId());
        dto.setDocument(DocumentDTO.fromEntity(request.getDocument(), null, false));
        dto.setApplicant(UserDTO.fromEntity(request.getApplicant()));
        dto.setStatus(request.getStatus());
        dto.setMessage(request.getMessage());
        dto.setCreatedAt(request.getCreatedAt());
        dto.setHandledAt(request.getHandledAt());
        // 处理人信息
        if (request.getHandledBy() != null) {
            dto.setHandledBy(UserDTO.fromEntity(request.getHandledBy()));
        }
        return dto;
    }
}
