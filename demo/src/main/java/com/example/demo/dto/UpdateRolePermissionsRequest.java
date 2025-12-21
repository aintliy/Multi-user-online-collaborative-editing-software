package com.example.demo.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class UpdateRolePermissionsRequest {
    @NotEmpty(message = "权限列表不能为空")
    private List<String> permissionCodes;
}
