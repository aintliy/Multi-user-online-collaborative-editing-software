package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 分享文档请求 DTO
 */
@Data
public class ShareDocumentRequest {

    @NotNull(message = "文档 ID 不能为空")
    private Long documentId;

    @NotBlank(message = "用户邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String userEmail;

    @NotBlank(message = "权限角色不能为空")
    private String role;  // EDITOR / VIEWER
}
