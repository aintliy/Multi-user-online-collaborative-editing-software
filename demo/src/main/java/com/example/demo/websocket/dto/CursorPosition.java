package com.example.demo.websocket.dto;

import lombok.Data;

/**
 * 光标位置 DTO
 */
@Data
public class CursorPosition {

    /**
     * 光标位置（字符索引）
     */
    private Integer position;

    /**
     * 选中范围的开始位置（可选）
     */
    private Integer selectionStart;

    /**
     * 选中范围的结束位置（可选）
     */
    private Integer selectionEnd;

    /**
     * 光标颜色（用于区分不同用户）
     */
    private String color;
}
