import axios, { type AxiosInstance, type AxiosRequestConfig } from "axios";
import { tokenStorage } from "@/utils/tokenStorage";

const baseURL = process.env.NEXT_PUBLIC_API_URL || "";
type ApiClientInstance = Omit<AxiosInstance, "get" | "post" | "put" | "delete"> & {
  get<T = unknown, D = unknown>(url: string, config?: AxiosRequestConfig<D>): Promise<T>;
  post<T = unknown, D = unknown>(url: string, data?: D, config?: AxiosRequestConfig<D>): Promise<T>;
  put<T = unknown, D = unknown>(url: string, data?: D, config?: AxiosRequestConfig<D>): Promise<T>;
  delete<T = unknown, D = unknown>(url: string, config?: AxiosRequestConfig<D>): Promise<T>;
};

const axiosInstance = axios.create({
  baseURL,
  timeout: 30000,
});

axiosInstance.interceptors.request.use((config) => {
  const token = tokenStorage.getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

axiosInstance.interceptors.response.use(
  (response) => {
    if (response.config.responseType === "blob") {
      return response;
    }
    const payload = response.data;
    if (payload?.code === 0) {
      return payload.data;
    }
    return Promise.reject(new Error(payload?.message || "请求失败"));
  },
  (error) => {
    const status = error.response?.status;
    if (status === 401) {
      tokenStorage.clear();
      if (typeof window !== "undefined") {
        window.location.href = "/auth/login";
      }
    }
    return Promise.reject(error);
  }
);

export const apiClient = axiosInstance as ApiClientInstance;

export const getApiBaseUrl = () => baseURL;
