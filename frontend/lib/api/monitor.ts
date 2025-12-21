import api from '@/lib/axios';

export interface SystemStats {
  totalUsers: number;
  totalDocuments: number;
  onlineUsers: number;
  activeDocuments: number;
}

export interface HealthStatus {
  status: string;
  database: string;
  memory: {
    total: string;
    used: string;
    free: string;
  };
  databaseError?: string;
}

export interface OperationLog {
  id: number;
  userId: number;
  action: string;
  targetType: string;
  targetId: number;
  detail: string;
  createdAt: string;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

// 获取系统统计信息
export const getSystemStats = (): Promise<SystemStats> => {
  return api.get('/api/admin/monitor/stats');
};

// 获取操作日志列表
export const getOperationLogs = (params: {
  page: number;
  size: number;
  userId?: number;
  action?: string;
  targetType?: string;
  startDate?: string;
  endDate?: string;
}): Promise<PageResult<OperationLog>> => {
  return api.get('/api/admin/monitor/operation-logs', { params });
};

// 获取系统健康状态
export const getHealthStatus = (): Promise<HealthStatus> => {
  return api.get('/api/admin/monitor/health');
};
