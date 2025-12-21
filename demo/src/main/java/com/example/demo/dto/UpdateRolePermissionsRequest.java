package com.example.demo.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateRolePermissionsRequest {
    @NotNull(message = "角色ID不能为空")
    private Long roleId;
    
    @NotEmpty(message = "权限列表不能为空")
    private List<Long> permissionIds;
}
