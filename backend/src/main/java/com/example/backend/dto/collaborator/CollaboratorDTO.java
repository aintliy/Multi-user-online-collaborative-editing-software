package com.example.backend.dto.collaborator;

import com.example.backend.entity.DocumentCollaborator;
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
    private Long userId;
    private String username;
    private String publicId;
    private String avatarUrl;
    private Long invitedById;
    private String invitedByName;
    private LocalDateTime createdAt;
    
    public static CollaboratorDTO fromEntity(DocumentCollaborator collaborator) {
        return CollaboratorDTO.builder()
                .id(collaborator.getId())
                .documentId(collaborator.getDocument().getId())
                .userId(collaborator.getUser().getId())
                .username(collaborator.getUser().getUsername())
                .publicId(collaborator.getUser().getPublicId())
                .avatarUrl(collaborator.getUser().getAvatarUrl())
                .invitedById(collaborator.getInvitedBy() != null ? collaborator.getInvitedBy().getId() : null)
                .invitedByName(collaborator.getInvitedBy() != null ? collaborator.getInvitedBy().getUsername() : null)
                .createdAt(collaborator.getCreatedAt())
                .build();
    }
}
