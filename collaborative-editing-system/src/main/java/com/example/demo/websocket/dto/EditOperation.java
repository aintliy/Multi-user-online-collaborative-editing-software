package com.example.demo.websocket.dto;

import lombok.Data;

/**
 * 编辑操作 DTO
 */
@Data
public class EditOperation {

    /**
     * 操作类型：insert / delete / replace
     */
    private String operation;

    /**
     * 操作位置（字符索引）
     */
    private Integer position;

    /**
     * 插入或替换的内容
     */
    private String content;

    /**
     * 删除的长度
     */
    private Integer length;

    /**
     * 操作范围（可选，用于 replace）
     */
    private Range range;

    @Data
    public static class Range {
        private Integer start;
        private Integer end;
    }
}
