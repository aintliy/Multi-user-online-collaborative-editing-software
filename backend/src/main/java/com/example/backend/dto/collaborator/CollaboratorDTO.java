package com.example.backend.dto.collaborator;

import com.example.backend.dto.auth.UserDTO;
import com.example.backend.entity.Document;
import com.example.backend.entity.DocumentCollaborator;
import com.example.backend.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 协作者DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollaboratorDTO {
    
    private Long id;
    private Long documentId;
    private UserDTO user;
    private String role;  // OWNER, EDITOR
    private Long invitedById;
    private String invitedByName;
    private LocalDateTime createdAt;
    
    public static CollaboratorDTO fromEntity(DocumentCollaborator collaborator) {
        return CollaboratorDTO.builder()
                .id(collaborator.getId())
                .documentId(collaborator.getDocument().getId())
                .user(UserDTO.fromEntity(collaborator.getUser()))
                .role("EDITOR")
                .invitedById(collaborator.getInvitedBy() != null ? collaborator.getInvitedBy().getId() : null)
                .invitedByName(collaborator.getInvitedBy() != null ? collaborator.getInvitedBy().getUsername() : null)
                .createdAt(collaborator.getCreatedAt())
                .build();
    }
    
    /**
     * 创建所有者DTO
     */
    public static CollaboratorDTO fromOwner(Document document) {
        User owner = document.getOwner();
        return CollaboratorDTO.builder()
                .id(0L)  // 所有者没有协作者记录ID
                .documentId(document.getId())
                .user(UserDTO.fromEntity(owner))
                .role("OWNER")
                .invitedById(null)
                .invitedByName(null)
                .createdAt(document.getCreatedAt())
                .build();
    }
}
