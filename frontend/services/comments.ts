import { apiClient } from "@/lib/apiClient";
import { Comment, PageResponse } from "@/types";

export interface CreateCommentPayload {
  documentId: number;
  content: string;
  parentId?: number;
}

export const commentsService = {
  list(documentId: number, page = 1, pageSize = 20) {
    return apiClient.get<PageResponse<Comment>>(`/api/comments/document/${documentId}`, {
      params: { page, pageSize },
    });
  },
  create(payload: CreateCommentPayload) {
    return apiClient.post<Comment>("/api/comments", payload);
  },
  remove(id: number) {
    return apiClient.delete<void>(`/api/comments/${id}`);
  },
  resolve(id: number) {
    return apiClient.put<void>(`/api/comments/${id}/resolve`);
  },
};
