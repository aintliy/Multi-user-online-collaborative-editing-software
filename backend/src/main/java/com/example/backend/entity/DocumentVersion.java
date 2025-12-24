package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 文档版本/提交记录实体类
 * 对应数据库表 document_versions
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "document_versions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"document_id", "version_no"}))
public class DocumentVersion {
    
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
     * 文档内部递增版本号：1,2,3,...
     */
    @Column(name = "version_no", nullable = false)
    private Integer versionNo;
    
    /**
     * 本次提交的完整内容快照
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    
    /**
     * 提交说明，类似Git commit message
     */
    @Column(name = "commit_message", length = 255)
    private String commitMessage;
    
    /**
     * 发起提交的用户
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
