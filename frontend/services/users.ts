import { apiClient } from "@/lib/apiClient";
import { Document, PageResponse, User } from "@/types";

export interface SearchUsersParams {
  keyword: string;
  limit?: number;
}

export const usersService = {
  getByPublicId(publicId: string) {
    return apiClient.get<User>(`/api/users/public/${publicId}`);
  },
  getById(id: number) {
    return apiClient.get<User>(`/api/users/${id}`);
  },
  search(params: SearchUsersParams) {
    return apiClient.get<User[]>("/api/users/search", { params });
  },
  uploadAvatar(file: File) {
    const formData = new FormData();
    formData.append("file", file);
    return apiClient.post<string>("/api/users/avatar", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });
  },
  getFriends() {
    return apiClient.get<User[]>("/api/users/friends");
  },
  getFriendRequests() {
    return apiClient.get<User[]>("/api/users/friends/requests");
  },
  sendFriendRequest(friendId: number) {
    return apiClient.post<void>(`/api/users/friends/${friendId}`);
  },
  acceptFriendRequest(requestId: number) {
    return apiClient.put<void>(`/api/users/friends/requests/${requestId}/accept`);
  },
  rejectFriendRequest(requestId: number) {
    return apiClient.put<void>(`/api/users/friends/requests/${requestId}/reject`);
  },
  removeFriend(friendId: number) {
    return apiClient.delete<void>(`/api/users/friends/${friendId}`);
  },
  getUserRepos(publicId: string, page = 1, pageSize = 10) {
    return apiClient.get<PageResponse<Document>>(`/api/users/${publicId}/repos`, {
      params: { page, pageSize },
    });
  },
};
