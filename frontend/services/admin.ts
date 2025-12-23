import { apiClient } from "@/lib/apiClient";
import { PageResponse, User } from "@/types";

export interface AdminUserQuery {
  page?: number;
  pageSize?: number;
  keyword?: string;
  role?: string;
  status?: string;
}

export interface SystemStats {
  userCount: number;
  documentCount: number;
  taskCount: number;
  activeCollaborationSessions: number;
}

export const adminService = {
  getUsers(params: AdminUserQuery = {}) {
    return apiClient.get<PageResponse<User>>("/api/admin/users", { params });
  },
  updateUserRole(userId: number, role: string) {
    return apiClient.put<void>(`/api/admin/users/${userId}/role`, null, {
      params: { role },
    });
  },
  updateUserStatus(userId: number, status: string) {
    return apiClient.put<void>(`/api/admin/users/${userId}/status`, null, {
      params: { status },
    });
  },
  resetUserPassword(userId: number, newPassword: string) {
    return apiClient.post<void>(`/api/admin/users/${userId}/reset-password`, null, {
      params: { newPassword },
    });
  },
  deleteUser(userId: number) {
    return apiClient.delete<void>(`/api/admin/users/${userId}`);
  },
  deleteDocument(documentId: number) {
    return apiClient.delete<void>(`/api/admin/documents/${documentId}`);
  },
  getStats() {
    return apiClient.get<SystemStats>("/api/admin/stats");
  },
};
