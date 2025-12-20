package com.example.demo.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 文档版本实体
 */
@Data
@TableName("document_versions")
public class DocumentVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long documentId;

    private Integer versionNo;

    private String content;

    private Long createdBy;

    private LocalDateTime createdAt;
}
