package com.example.backend.dto.document;

import com.example.backend.entity.DocumentShareLink;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文档分享链接DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShareLinkDTO {
    
    private Long id;
    private Long documentId;
    private String documentTitle;
    private String token;
    private String shareUrl;
    private Boolean isUsed;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    
    public static ShareLinkDTO fromEntity(DocumentShareLink link) {
        return ShareLinkDTO.builder()
                .id(link.getId())
                .documentId(link.getDocument().getId())
                .documentTitle(link.getDocument().getTitle())
                .token(link.getToken())
                .shareUrl("share:" + link.getToken())  // 特殊格式，仅限聊天窗口识别
                .isUsed(link.getIsUsed())
                .expiresAt(link.getExpiresAt())
                .createdAt(link.getCreatedAt())
                .build();
    }
}
