import { apiClient } from "@/lib/apiClient";
import { Notification, PageResponse } from "@/types";

export interface NotificationQuery {
  page?: number;
  pageSize?: number;
  read?: boolean;
}

export const notificationsService = {
  list(params: NotificationQuery = {}) {
    return apiClient.get<PageResponse<Notification>>("/api/notifications", { params });
  },
  unreadCount() {
    return apiClient.get<number>("/api/notifications/unread-count");
  },
  markAsRead(id: number) {
    return apiClient.put<void>(`/api/notifications/${id}/read`);
  },
  markAllAsRead() {
    return apiClient.put<void>("/api/notifications/read-all");
  },
};
