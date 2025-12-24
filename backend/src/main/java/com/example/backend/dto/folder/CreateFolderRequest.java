package com.example.backend.dto.folder;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建文件夹请求DTO
 */
@Data
public class CreateFolderRequest {
    
    @NotBlank(message = "文件夹名称不能为空")
    private String name;
    
    private Long parentId;
}
