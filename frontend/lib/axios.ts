import axios from 'axios';
import API_BASE_URL from '@/config/api';

// 创建 axios 实例
const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器 - 添加 token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器 - 统一处理响应
api.interceptors.response.use(
  (response) => {
    const { code, message, data } = response.data;
    
    if (code === 0) {
      return data;
    } else {
      // 处理业务错误
      throw new Error(message || '请求失败');
    }
  },
  (error) => {
    // 处理 HTTP 错误
    if (error.response) {
      const { status } = error.response;
      
      if (status === 401) {
        // 未认证，跳转登录
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        window.location.href = '/login';
      } else if (status === 403) {
        throw new Error('没有权限访问');
      } else if (status === 404) {
        throw new Error('请求的资源不存在');
      } else if (status >= 500) {
        throw new Error('服务器错误，请稍后重试');
      }
    } else if (error.request) {
      throw new Error('网络错误，请检查网络连接');
    }
    
    return Promise.reject(error);
  }
);

export default api;
