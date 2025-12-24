package com.example.backend.dto.folder;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新文件夹请求DTO
 */
@Data
public class UpdateFolderRequest {
    
    @NotBlank(message = "文件夹名称不能为空")
    private String name;
}
