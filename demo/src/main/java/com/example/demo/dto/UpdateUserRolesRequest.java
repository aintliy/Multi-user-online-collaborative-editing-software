package com.example.demo.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class UpdateUserRolesRequest {
    @NotEmpty(message = "角色列表不能为空")
    private List<String> roleCodes;
}
