package com.example.demo.entity;

import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 文档协作申请实体（加入工作区申请）
 * 对应数据库表：document_workspace_requests
 */
@Data
@TableName("document_workspace_requests")
public class DocumentWorkspaceRequest {
    
    /**
     * 申请ID（主键）
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 文档ID
     */
    @TableField("document_id")
    private Long documentId;
    
    /**
     * 申请人ID
     */
    @TableField("applicant_id")
    private Long applicantId;
    
    /**
     * 申请状态：PENDING-待处理，APPROVED-已批准，REJECTED-已拒绝
     */
    @TableField("status")
    private String status;
    
    /**
     * 申请理由
     */
    @TableField("message")
    private String message;
    
    /**
     * 创建时间
     */
    @TableField("created_at")
    private OffsetDateTime createdAt;
    
    /**
     * 处理时间
     */
    @TableField("handled_at")
    private OffsetDateTime handledAt;
    
    /**
     * 处理人ID（通常为文档所有者）
     */
    @TableField("handled_by")
    private Long handledBy;
}
