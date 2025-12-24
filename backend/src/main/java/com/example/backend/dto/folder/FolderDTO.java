package com.example.backend.dto.folder;

import com.example.backend.entity.DocumentFolder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件夹DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolderDTO {
    
    private Long id;
    private String name;
    private Long parentId;
    private Long ownerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<FolderDTO> children;
    
    public static FolderDTO fromEntity(DocumentFolder folder) {
        return FolderDTO.builder()
                .id(folder.getId())
                .name(folder.getName())
                .parentId(folder.getParent() != null ? folder.getParent().getId() : null)
                .ownerId(folder.getOwner().getId())
                .createdAt(folder.getCreatedAt())
                .updatedAt(folder.getUpdatedAt())
                .build();
    }
}
