import { apiClient } from "@/lib/apiClient";
import {
  Collaborator,
  Document,
  DocumentFilters,
  DocumentVersion,
  PageResponse,
} from "@/types";

export interface DocumentListParams extends DocumentFilters {
  page?: number;
  pageSize?: number;
  tag?: string;
  visibility?: "public" | "private";
}

export interface CreateDocumentPayload {
  title: string;
  type?: string;
  visibility?: "public" | "private";
  tags?: string;
}

export interface UpdateDocumentPayload {
  title?: string;
  visibility?: "public" | "private";
  tags?: string;
}

export interface CommitDocumentPayload {
  content: string;
  commitMessage?: string;
}

export interface AddCollaboratorPayload {
  collaboratorUserId: number;
  role: "VIEWER" | "EDITOR";
}

export const documentsService = {
  list(params: DocumentListParams = {}) {
    return apiClient.get<PageResponse<Document>>("/api/documents", { params });
  },
  get(id: number) {
    return apiClient.get<Document>(`/api/documents/${id}`);
  },
  create(payload: CreateDocumentPayload) {
    return apiClient.post<Document>("/api/documents", payload);
  },
  update(id: number, payload: UpdateDocumentPayload) {
    return apiClient.put<void>(`/api/documents/${id}`, payload);
  },
  remove(id: number) {
    return apiClient.delete<void>(`/api/documents/${id}`);
  },
  clone(id: number) {
    return apiClient.post<Document>(`/api/documents/${id}/clone`);
  },
  commit(id: number, payload: CommitDocumentPayload) {
    return apiClient.post<DocumentVersion>(`/api/documents/${id}/commit`, payload);
  },
  getVersions(id: number) {
    return apiClient.get<DocumentVersion[]>(`/api/documents/${id}/versions`);
  },
  rollback(id: number, versionNumber: number) {
    return apiClient.post<DocumentVersion>(`/api/documents/${id}/rollback`, null, {
      params: { versionNumber },
    });
  },
  getCollaborators(id: number) {
    return apiClient.get<Collaborator[]>(`/api/documents/${id}/collaborators`);
  },
  addCollaborator(id: number, payload: AddCollaboratorPayload) {
    return apiClient.post<void>(`/api/documents/${id}/collaborators`, payload);
  },
  removeCollaborator(id: number, collaboratorId: number) {
    return apiClient.delete<void>(`/api/documents/${id}/collaborators/${collaboratorId}`);
  },
  updateCollaborator(id: number, collaboratorId: number, role: "VIEWER" | "EDITOR") {
    return apiClient.put<void>(`/api/documents/${id}/collaborators/${collaboratorId}`, null, {
      params: { role },
    });
  },
  importDocument(formData: FormData) {
    return apiClient.post<Document>("/api/documents/import", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });
  },
  exportDocument(id: number, format = "markdown") {
    return apiClient.get<Blob>(`/api/documents/${id}/export`, {
      params: { format },
      responseType: "blob",
    });
  },
};
