import axiosInstance from '../axios';

export interface Notification {
  id: number;
  userId: number;
  type: 'COMMENT' | 'TASK' | 'PERMISSION';
  title: string;
  content: string;
  relatedId?: number;
  isRead: boolean;
  createdAt: string;
}

export interface NotificationVO extends Notification {
  documentTitle?: string;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

/**
 * 获取通知列表（分页）
 */
export const getNotifications = async (
  page: number = 1,
  size: number = 10,
  isRead?: boolean
): Promise<PageResult<NotificationVO>> => {
  const response = await axiosInstance.get('/api/notifications', {
    params: { page, size, isRead },
  });
  return response.data.data;
};

/**
 * 获取未读通知数量
 */
export const getUnreadCount = async (): Promise<number> => {
  const response = await axiosInstance.get('/api/notifications/unread-count');
  return response.data.data;
};

/**
 * 标记通知为已读
 */
export const markAsRead = async (id: number): Promise<void> => {
  await axiosInstance.put(`/api/notifications/${id}/read`);
};

/**
 * 标记所有通知为已读
 */
export const markAllAsRead = async (): Promise<void> => {
  await axiosInstance.put('/api/notifications/read-all');
};

/**
 * 删除通知
 */
export const deleteNotification = async (id: number): Promise<void> => {
  await axiosInstance.delete(`/api/notifications/${id}`);
};
