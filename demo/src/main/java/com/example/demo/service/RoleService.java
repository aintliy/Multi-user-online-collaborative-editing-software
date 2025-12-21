package com.example.demo.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.BusinessException;
import com.example.demo.common.ErrorCode;
import com.example.demo.dto.PermissionVO;
import com.example.demo.dto.RoleVO;
import com.example.demo.dto.UserVO;
import com.example.demo.entity.Permission;
import com.example.demo.entity.Role;
import com.example.demo.entity.RolePermission;
import com.example.demo.entity.User;
import com.example.demo.entity.UserRole;
import com.example.demo.mapper.PermissionMapper;
import com.example.demo.mapper.RoleMapper;
import com.example.demo.mapper.RolePermissionMapper;
import com.example.demo.mapper.UserMapper;
import com.example.demo.mapper.UserRoleMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoleService {
    
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final UserRoleMapper userRoleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final UserMapper userMapper;
    
    /**
     * 获取所有角色列表
     */
    public List<RoleVO> getAllRoles() {
        List<Role> roles = roleMapper.selectList(null);
        return roles.stream()
            .map(this::convertToRoleVO)
            .collect(Collectors.toList());
    }
    
    /**
     * 获取所有权限列表
     */
    public List<PermissionVO> getAllPermissions() {
        List<Permission> permissions = permissionMapper.selectList(null);
        return permissions.stream()
            .map(this::convertToPermissionVO)
            .collect(Collectors.toList());
    }
    
    /**
     * 获取角色的权限列表
     */
    public List<PermissionVO> getRolePermissions(Long roleId) {
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
        
        List<RolePermission> rolePermissions = rolePermissionMapper.selectList(
            new LambdaQueryWrapper<RolePermission>()
                .eq(RolePermission::getRoleId, roleId)
        );
        
        return rolePermissions.stream()
            .map(rp -> permissionMapper.selectById(rp.getPermissionId()))
            .filter(p -> p != null)
            .map(this::convertToPermissionVO)
            .collect(Collectors.toList());
    }
    
    /**
     * 更新角色的权限
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateRolePermissions(Long roleId, List<Long> permissionIds) {
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
        
        // 删除原有权限关联
        rolePermissionMapper.delete(
            new LambdaQueryWrapper<RolePermission>()
                .eq(RolePermission::getRoleId, roleId)
        );
        
        // 添加新的权限关联
        for (Long permissionId : permissionIds) {
            Permission permission = permissionMapper.selectById(permissionId);
            if (permission != null) {
                RolePermission rolePermission = new RolePermission();
                rolePermission.setRoleId(roleId);
                rolePermission.setPermissionId(permissionId);
                rolePermissionMapper.insert(rolePermission);
            }
        }
    }
    
    /**
     * 获取用户的角色列表
     */
    public List<RoleVO> getUserRoles(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        List<UserRole> userRoles = userRoleMapper.selectList(
            new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId)
        );
        
        return userRoles.stream()
            .map(ur -> roleMapper.selectById(ur.getRoleId()))
            .filter(r -> r != null)
            .map(this::convertToRoleVO)
            .collect(Collectors.toList());
    }
    
    /**
     * 更新用户的角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateUserRoles(Long userId, List<Long> roleIds) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        // 删除原有角色关联
        userRoleMapper.delete(
            new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId)
        );
        
        // 添加新的角色关联
        for (Long roleId : roleIds) {
            Role role = roleMapper.selectById(roleId);
            if (role != null) {
                UserRole userRole = new UserRole();
                userRole.setUserId(userId);
                userRole.setRoleId(roleId);
                userRoleMapper.insert(userRole);
            }
        }
    }
    
    /**
     * 获取所有用户（管理员）
     */
    public IPage<UserVO> getAllUsers(int page, int size, String keyword) {
        Page<User> userPage = new Page<>(page, size);
        
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w
                .like(User::getUsername, keyword)
                .or()
                .like(User::getEmail, keyword)
            );
        }
        
        IPage<User> result = userMapper.selectPage(userPage, wrapper);
        
        return result.convert(user -> {
            UserVO vo = new UserVO();
            vo.setId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setEmail(user.getEmail());
            vo.setPhone(user.getPhone());
            vo.setAvatarUrl(user.getAvatarUrl());
            vo.setProfile(user.getProfile());
            vo.setStatus(user.getStatus());
            
            // 填充角色信息
            List<UserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>()
                    .eq(UserRole::getUserId, user.getId())
            );
            
            List<String> roles = userRoles.stream()
                .map(ur -> {
                    Role role = roleMapper.selectById(ur.getRoleId());
                    return role != null ? role.getCode() : null;
                })
                .filter(code -> code != null)
                .collect(Collectors.toList());
            
            vo.setRoles(roles);
            
            return vo;
        });
    }
    
    /**
     * 禁用/启用用户
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long userId, String status) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        user.setStatus(status);
        userMapper.updateById(user);
    }
    
    private RoleVO convertToRoleVO(Role role) {
        RoleVO vo = new RoleVO();
        vo.setId(role.getId());
        vo.setCode(role.getCode());
        vo.setName(role.getName());
        vo.setDescription(role.getDescription());
        
        // 填充权限列表
        List<RolePermission> rolePermissions = rolePermissionMapper.selectList(
            new LambdaQueryWrapper<RolePermission>()
                .eq(RolePermission::getRoleId, role.getId())
        );
        
        List<PermissionVO> permissions = rolePermissions.stream()
            .map(rp -> permissionMapper.selectById(rp.getPermissionId()))
            .filter(p -> p != null)
            .map(this::convertToPermissionVO)
            .collect(Collectors.toList());
        
        vo.setPermissions(permissions);
        
        return vo;
    }
    
    private PermissionVO convertToPermissionVO(Permission permission) {
        PermissionVO vo = new PermissionVO();
        vo.setId(permission.getId());
        vo.setCode(permission.getCode());
        vo.setName(permission.getName());
        vo.setDescription(permission.getDescription());
        return vo;
    }
}
