package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 文档邀请链接实体类
 * 对应数据库表 document_invite_links
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "document_invite_links")
public class DocumentInviteLink {
    
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
     * 邀请链接中的随机token
     */
    @Column(nullable = false, unique = true, length = 64)
    private String token;
    
    /**
     * 最大可用次数（NULL表示无限制）
     */
    @Column(name = "max_uses")
    private Integer maxUses;
    
    /**
     * 已使用次数
     */
    @Column(name = "used_count")
    @Builder.Default
    private Integer usedCount = 0;
    
    /**
     * 过期时间
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    /**
     * 创建人
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
