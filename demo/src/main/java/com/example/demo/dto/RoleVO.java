package com.example.demo.dto;

import java.util.List;

import lombok.Data;

@Data
public class RoleVO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private List<PermissionVO> permissions;
}
