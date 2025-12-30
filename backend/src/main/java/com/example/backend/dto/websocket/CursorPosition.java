package com.example.backend.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 光标位置DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CursorPosition {
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户昵称
     */
    private String nickname;
    
    /**
     * 用户颜色标识
     */
    private String color;
    
    /**
     * 光标位置
     */
    private Integer position;

    /**
     * 行号（兼容前端编辑器光标结构）
     */
    private Integer line;

    /**
     * 列号（兼容前端编辑器光标结构）
     */
    private Integer column;
    
    /**
     * 选区起始位置
     */
    private Integer selectionStart;
    
    /**
     * 选区结束位置
     */
    private Integer selectionEnd;
}
