import api from '@/lib/axios';
import { UserVO } from './auth';

// 上传头像
export const uploadAvatar = (file: File): Promise<{ avatarUrl: string }> => {
  const formData = new FormData();
  formData.append('file', file);
  return api.post('/api/users/me/avatar', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
};

// 更新用户资料（UserController的接口）
export interface UpdateUserProfileRequest {
  username?: string;
  phone?: string;
  profile?: string;
}

export const updateUserProfile = (data: UpdateUserProfileRequest): Promise<UserVO> => {
  return api.put('/api/users/me', data);
};
