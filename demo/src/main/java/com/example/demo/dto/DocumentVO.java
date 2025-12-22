package com.example.demo.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 文档视图对象
 */
@Data
public class DocumentVO {
    
    private Long id;
    
    private String title;
    
    private Long ownerId;
    
    private String ownerName;
    
    private String content;
    
    private String docType;
    
    private String visibility;
    
    private String tags;
    
    private String status;
    
    /**
     * 克隆来源文档ID
     */
    private Long forkedFromId;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    /**
     * 当前用户是否为所有者
     */
    private Boolean isOwner;
    
    /**
     * 当前用户是否具有编辑权限
     */
    private Boolean canEdit;
}
