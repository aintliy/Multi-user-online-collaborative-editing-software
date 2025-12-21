import axiosInstance from '../axios';

export interface Task {
  id: number;
  title: string;
  description: string;
  documentId?: number;
  assigneeId: number;
  assigneeName?: string;
  status: 'TODO' | 'DOING' | 'DONE';
  dueDate?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTaskRequest {
  title: string;
  description: string;
  documentId?: number;
  assigneeId: number;
  dueDate?: string;
}

export interface UpdateTaskRequest {
  title?: string;
  description?: string;
  status?: 'TODO' | 'DOING' | 'DONE';
  dueDate?: string;
}

export interface TaskVO extends Task {
  assigneeName: string;
  documentTitle?: string;
}

/**
 * 创建任务
 */
export const createTask = async (data: CreateTaskRequest): Promise<TaskVO> => {
  const response = await axiosInstance.post('/api/tasks', data);
  return response.data.data;
};

/**
 * 获取我的任务列表
 */
export const getMyTasks = async (status?: string): Promise<TaskVO[]> => {
  const response = await axiosInstance.get('/api/tasks/my', {
    params: { status },
  });
  return response.data.data;
};

/**
 * 获取任务详情
 */
export const getTaskById = async (id: number): Promise<TaskVO> => {
  const response = await axiosInstance.get(`/api/tasks/${id}`);
  return response.data.data;
};

/**
 * 更新任务
 */
export const updateTask = async (id: number, data: UpdateTaskRequest): Promise<TaskVO> => {
  const response = await axiosInstance.put(`/api/tasks/${id}`, data);
  return response.data.data;
};

/**
 * 删除任务
 */
export const deleteTask = async (id: number): Promise<void> => {
  await axiosInstance.delete(`/api/tasks/${id}`);
};
