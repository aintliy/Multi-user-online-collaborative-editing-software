package com.example.backend.dto.document;

import com.example.backend.entity.DocumentVersion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文档版本DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentVersionDTO {
    
    private Long id;
    private Long documentId;
    private Integer versionNo;
    private String content;
    private String commitMessage;
    private Long createdById;
    private String createdByName;
    private LocalDateTime createdAt;
    
    public static DocumentVersionDTO fromEntity(DocumentVersion version) {
        return DocumentVersionDTO.builder()
                .id(version.getId())
                .documentId(version.getDocument().getId())
                .versionNo(version.getVersionNo())
                .content(version.getContent())
                .commitMessage(version.getCommitMessage())
                .createdById(version.getCreatedBy() != null ? version.getCreatedBy().getId() : null)
                .createdByName(version.getCreatedBy() != null ? version.getCreatedBy().getUsername() : null)
                .createdAt(version.getCreatedAt())
                .build();
    }
}
