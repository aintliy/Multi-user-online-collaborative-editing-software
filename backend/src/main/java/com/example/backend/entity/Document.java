package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 文档实体类
 * 对应数据库表 documents
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "documents")
public class Document {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 255)
    private String title;
    
    /**
     * 文档所有者
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
    
    /**
     * 当前最新「已提交」内容
     */
    @Column(columnDefinition = "TEXT")
    private String content;
    
    /**
     * 文档类型: markdown / txt
     */
    @Column(name = "doc_type", length = 20)
    @Builder.Default
    private String docType = "markdown";
    
    /**
     * 可见性: private / public
     */
    @Column(length = 20)
    @Builder.Default
    private String visibility = "private";
    
    /**
     * 标签，逗号分隔
     */
    @Column(length = 255)
    private String tags;
    
    /**
     * 所属文件夹ID
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private DocumentFolder folder;
    
    /**
     * 文档状态
     */
    @Column(length = 20)
    @Builder.Default
    private String status = "ACTIVE";
    
    /**
     * 物理文件存储相对路径，格式: {ownerId}/{folderId}/
     */
    @Column(name = "storage_path", length = 512)
    private String storagePath;
    
    /**
     * 若为克隆副本，则指向原始文档ID
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forked_from_id")
    private Document forkedFrom;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
