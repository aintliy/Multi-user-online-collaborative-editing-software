import api from '@/lib/axios';

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  phone?: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  user: UserVO;
}

export interface UserVO {
  id: number;
  username: string;
  email: string;
  phone?: string;
  avatarUrl?: string;
  profile?: string;
  status: string;
  roles?: string[];
  createdAt: string;
  updatedAt: string;
}

// 用户注册
export const register = (data: RegisterRequest): Promise<UserVO> => {
  return api.post('/api/auth/register', data);
};

// 用户登录
export const login = (data: LoginRequest): Promise<LoginResponse> => {
  return api.post('/api/auth/login', data);
};

// 获取当前用户信息
export const getCurrentUser = (): Promise<UserVO> => {
  return api.get('/api/auth/me');
};

// 用户登出
export const logout = (): Promise<void> => {
  return api.post('/api/auth/logout');
};

// 忘记密码
export const forgotPassword = (email: string): Promise<void> => {
  return api.post('/api/auth/forgot-password', { email });
};

// 重置密码
export const resetPassword = (data: { token: string; newPassword: string }): Promise<void> => {
  return api.post('/api/auth/reset-password', data);
};

// 更新用户资料
export interface UpdateProfileRequest {
  username?: string;
  phone?: string;
  profile?: string;
}

export const updateProfile = (data: UpdateProfileRequest): Promise<UserVO> => {
  return api.put('/api/auth/profile', data);
};
