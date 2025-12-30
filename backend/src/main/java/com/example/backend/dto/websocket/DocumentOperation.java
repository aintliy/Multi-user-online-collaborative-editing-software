package com.example.backend.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档编辑操作DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentOperation {
    
    /**
     * 操作类型: INSERT, DELETE, REPLACE, RETAIN
     */
    private String type;
    
    /**
     * 操作位置
     */
    private Integer position;
    
    /**
     * 插入/替换的文本
     */
    private String text;

    /**
     * 全量内容（兼容前端 replace 行为）
     */
    private String content;
    
    /**
     * 删除的字符数
     */
    private Integer length;
    
    /**
     * 操作的版本号
     */
    private Long version;
    
    /**
     * 客户端ID
     */
    private String clientId;
}
