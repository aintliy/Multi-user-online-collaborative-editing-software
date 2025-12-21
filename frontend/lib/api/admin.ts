import axiosInstance from '../axios';

export interface UserVO {
  id: number;
  username: string;
  email: string;
  avatar?: string;
  status: 'ACTIVE' | 'INACTIVE';
  roles: string[];
  createdAt: string;
}

export interface RoleVO {
  id: number;
  name: string;
  description: string;
  createdAt: string;
}

export interface PermissionVO {
  id: number;
  name: string;
  code: string;
  description: string;
  createdAt: string;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

export interface UpdateUserRolesRequest {
  userId: number;
  roleIds: number[];
}

export interface UpdateRolePermissionsRequest {
  roleId: number;
  permissionIds: number[];
}

/**
 * 获取所有用户列表（分页）
 */
export const getAllUsers = async (
  page: number = 1,
  size: number = 10,
  keyword?: string
): Promise<PageResult<UserVO>> => {
  const response = await axiosInstance.get('/api/admin/users', {
    params: { page, size, keyword },
  });
  return response.data.data;
};

/**
 * 获取所有角色
 */
export const getAllRoles = async (): Promise<RoleVO[]> => {
  const response = await axiosInstance.get('/api/admin/roles');
  return response.data.data;
};

/**
 * 获取所有权限
 */
export const getAllPermissions = async (): Promise<PermissionVO[]> => {
  const response = await axiosInstance.get('/api/admin/permissions');
  return response.data.data;
};

/**
 * 获取用户的角色列表
 */
export const getUserRoles = async (userId: number): Promise<RoleVO[]> => {
  const response = await axiosInstance.get(`/api/admin/users/${userId}/roles`);
  return response.data.data;
};

/**
 * 获取角色的权限列表
 */
export const getRolePermissions = async (roleId: number): Promise<PermissionVO[]> => {
  const response = await axiosInstance.get(`/api/admin/roles/${roleId}/permissions`);
  return response.data.data;
};

/**
 * 更新用户角色
 */
export const updateUserRoles = async (data: UpdateUserRolesRequest): Promise<void> => {
  await axiosInstance.put('/api/admin/users/roles', data);
};

/**
 * 更新角色权限
 */
export const updateRolePermissions = async (data: UpdateRolePermissionsRequest): Promise<void> => {
  await axiosInstance.put('/api/admin/roles/permissions', data);
};

/**
 * 更新用户状态
 */
export const updateUserStatus = async (userId: number, status: 'ACTIVE' | 'INACTIVE'): Promise<void> => {
  await axiosInstance.put(`/api/admin/users/${userId}/status`, { status });
};
