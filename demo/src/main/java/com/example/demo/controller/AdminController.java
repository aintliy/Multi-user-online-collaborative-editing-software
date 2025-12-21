package com.example.demo.controller;

import java.util.List;
import java.util.Map;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.common.Result;
import com.example.demo.dto.PermissionVO;
import com.example.demo.dto.RoleVO;
import com.example.demo.dto.UpdateRolePermissionsRequest;
import com.example.demo.dto.UpdateUserRolesRequest;
import com.example.demo.dto.UserVO;
import com.example.demo.service.RoleService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    
    private final RoleService roleService;
    
    /**
     * 获取所有用户列表（分页）
     */
    @GetMapping("/users")
    public Result<IPage<UserVO>> getAllUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        IPage<UserVO> users = roleService.getAllUsers(page, size, keyword);
        return Result.success(users);
    }
    
    /**
     * 获取用户的角色列表
     */
    @GetMapping("/users/{userId}/roles")
    public Result<List<RoleVO>> getUserRoles(@PathVariable Long userId) {
        List<RoleVO> roles = roleService.getUserRoles(userId);
        return Result.success(roles);
    }
    
    /**
     * 更新用户的角色
     */
    @PutMapping("/users/{userId}/roles")
    public Result<Void> updateUserRoles(
            @PathVariable Long userId,
            @Validated @RequestBody UpdateUserRolesRequest request) {
        roleService.updateUserRoles(userId, request);
        return Result.success(null);
    }
    
    /**
     * 禁用/启用用户
     */
    @PutMapping("/users/{userId}/status")
    public Result<Void> updateUserStatus(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        roleService.updateUserStatus(userId, status);
        return Result.success(null);
    }
    
    /**
     * 获取所有角色列表
     */
    @GetMapping("/roles")
    public Result<List<RoleVO>> getAllRoles() {
        List<RoleVO> roles = roleService.getAllRoles();
        return Result.success(roles);
    }
    
    /**
     * 获取角色的权限列表
     */
    @GetMapping("/roles/{roleId}/permissions")
    public Result<List<PermissionVO>> getRolePermissions(@PathVariable Long roleId) {
        List<PermissionVO> permissions = roleService.getRolePermissions(roleId);
        return Result.success(permissions);
    }
    
    /**
     * 更新角色的权限
     */
    @PutMapping("/roles/{roleId}/permissions")
    public Result<Void> updateRolePermissions(
            @PathVariable Long roleId,
            @Validated @RequestBody UpdateRolePermissionsRequest request) {
        roleService.updateRolePermissions(roleId, request);
        return Result.success(null);
    }
    
    /**
     * 获取所有权限列表
     */
    @GetMapping("/permissions")
    public Result<List<PermissionVO>> getAllPermissions() {
        List<PermissionVO> permissions = roleService.getAllPermissions();
        return Result.success(permissions);
    }
}
