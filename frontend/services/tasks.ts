import { apiClient } from "@/lib/apiClient";
import { PageResponse, Task, TaskPriority, TaskStatus } from "@/types";

export interface TaskListParams {
  page?: number;
  pageSize?: number;
  status?: TaskStatus;
  priority?: TaskPriority;
  assigneeId?: number;
  creatorId?: number;
  relatedDocId?: number;
}

export interface CreateTaskPayload {
  title: string;
  description?: string;
  documentId?: number;
  assigneeId?: number;
  priority?: TaskPriority;
  dueDate?: string;
}

export interface UpdateTaskPayload extends Partial<CreateTaskPayload> {
  status?: TaskStatus;
}

export const tasksService = {
  list(params: TaskListParams = {}) {
    return apiClient.get<PageResponse<Task>>("/api/tasks", { params });
  },
  get(id: number) {
    return apiClient.get<Task>(`/api/tasks/${id}`);
  },
  create(payload: CreateTaskPayload) {
    return apiClient.post<Task>("/api/tasks", payload);
  },
  update(id: number, payload: UpdateTaskPayload) {
    return apiClient.put<Task>(`/api/tasks/${id}`, payload);
  },
  remove(id: number) {
    return apiClient.delete<void>(`/api/tasks/${id}`);
  },
};
