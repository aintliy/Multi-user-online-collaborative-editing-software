package com.example.backend.dto.document;

import com.example.backend.entity.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文档信息DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentDTO {
    
    private Long id;
    private String title;
    private Long ownerId;
    private String ownerName;
    private String ownerPublicId;
    private String visibility;
    private String docType;
    private Long forkedFromId;
    private String content;
    private String tags;
    private Long folderId;
    private String folderName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isOwner;
    private Boolean canEdit;
    
    public static DocumentDTO fromEntity(Document document, Long currentUserId, boolean canEdit) {
        return DocumentDTO.builder()
                .id(document.getId())
                .title(document.getTitle())
                .ownerId(document.getOwner().getId())
                .ownerName(document.getOwner().getUsername())
                .ownerPublicId(document.getOwner().getPublicId())
                .visibility(document.getVisibility())
                .docType(document.getDocType())
                .forkedFromId(document.getForkedFrom() != null ? document.getForkedFrom().getId() : null)
                .content(document.getContent())
                .tags(document.getTags())
                .folderId(document.getFolder() != null ? document.getFolder().getId() : null)
                .folderName(document.getFolder() != null ? document.getFolder().getName() : null)
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .isOwner(document.getOwner().getId().equals(currentUserId))
                .canEdit(canEdit)
                .build();
    }
}
