package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 文档分享链接实体类（一次性使用，仅限好友聊天）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "document_share_links")
public class DocumentShareLink {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 关联的文档
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;
    
    /**
     * 分享链接token
     */
    @Column(nullable = false, unique = true, length = 64)
    private String token;
    
    /**
     * 创建人（分享者）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    
    /**
     * 是否已使用
     */
    @Column(name = "is_used")
    @Builder.Default
    private Boolean isUsed = false;
    
    /**
     * 使用人
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "used_by")
    private User usedBy;
    
    /**
     * 使用时间
     */
    @Column(name = "used_at")
    private LocalDateTime usedAt;
    
    /**
     * 过期时间（24小时后过期）
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
