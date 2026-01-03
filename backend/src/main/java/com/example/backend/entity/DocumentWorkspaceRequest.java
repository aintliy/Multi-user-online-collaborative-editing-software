package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 文档协作请求实体类
 * 对应数据库表 document_workspace_requests
 * 支持两种类型: APPLY-用户申请加入, INVITE-所有者邀请
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "document_workspace_requests")
public class DocumentWorkspaceRequest {
    
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
     * 申请人/被邀请人
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false)
    private User applicant;
    
    /**
     * 请求类型: APPLY-用户申请加入, INVITE-所有者邀请
     */
    @Column(length = 20, nullable = false)
    @Builder.Default
    private String type = "APPLY";
    
    /**
     * 申请状态: PENDING / APPROVED / REJECTED
     */
    @Column(length = 20, nullable = false)
    @Builder.Default
    private String status = "PENDING";
    
    /**
     * 申请理由
     */
    @Column(length = 255)
    private String message;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "handled_at")
    private LocalDateTime handledAt;
    
    /**
     * 处理人（通常为owner）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handled_by")
    private User handledBy;
}
