import axios from 'axios';
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios';
import { message } from 'antd';
import type { ApiResponse } from '../types';

// 创建axios实例
const request: AxiosInstance = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器
request.interceptors.request.use(
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

// 响应拦截器
request.interceptors.response.use(
  (response: AxiosResponse<ApiResponse<any>>) => {
    const { data } = response;
    if (data.code === 0) {
      return response;
    }
    // 业务错误
    message.error(data.message || '请求失败');
    return Promise.reject(new Error(data.message));
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response;
      switch (status) {
        case 401:
          message.error('登录已过期，请重新登录');
          localStorage.removeItem('token');
          localStorage.removeItem('user');
          window.location.href = '/login';
          break;
        case 403:
          message.error('没有权限访问');
          break;
        case 404:
          message.error('请求的资源不存在');
          break;
        case 500:
          message.error('服务器错误');
          break;
        default:
          message.error(data?.message || '请求失败');
      }
    } else if (error.message.includes('timeout')) {
      message.error('请求超时');
    } else {
      message.error('网络错误');
    }
    return Promise.reject(error);
  }
);

// 封装GET请求
export const get = async <T>(url: string, config?: AxiosRequestConfig): Promise<T> => {
  const response = await request.get<ApiResponse<T>>(url, config);
  return response.data.data;
};

// 封装POST请求
export const post = async <T>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> => {
  const response = await request.post<ApiResponse<T>>(url, data, config);
  return response.data.data;
};

// 封装PUT请求
export const put = async <T>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> => {
  const response = await request.put<ApiResponse<T>>(url, data, config);
  return response.data.data;
};

// 封装DELETE请求
export const del = async <T>(url: string, config?: AxiosRequestConfig): Promise<T> => {
  const response = await request.delete<ApiResponse<T>>(url, config);
  return response.data.data;
};

// 文件上传
export const upload = async <T>(url: string, file: File, fieldName: string = 'file'): Promise<T> => {
  const formData = new FormData();
  formData.append(fieldName, file);
  const response = await request.post<ApiResponse<T>>(url, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
  return response.data.data;
};

// 文件下载
export const download = async (url: string, filename: string): Promise<void> => {
  const response = await request.get(url, {
    responseType: 'blob',
  });
  const blob = new Blob([response.data]);
  const link = document.createElement('a');
  link.href = URL.createObjectURL(blob);
  link.download = filename;
  link.click();
  URL.revokeObjectURL(link.href);
};

export default request;
