import api from '@/lib/axios';

export interface Document {
  id: number;
  title: string;
  ownerId: number;
  ownerName: string;
  content: string;
  docType: string;
  status: string;
  permission: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateDocumentRequest {
  title: string;
  content?: string;
  docType?: string;
}

export interface UpdateDocumentRequest {
  title?: string;
  content?: string;
}

export interface ShareDocumentRequest {
  documentId: number;
  userEmail: string;
  role: 'EDITOR' | 'VIEWER';
}

export interface PageResult<T> {
  records: T[];
  current: number;
  size: number;
  total: number;
}

// 创建文档
export const createDocument = (data: CreateDocumentRequest): Promise<Document> => {
  return api.post('/api/documents', data);
};

// 获取文档列表
export const getDocuments = (page = 1, size = 10): Promise<PageResult<Document>> => {
  return api.get('/api/documents', { params: { page, size } });
};

// 获取文档详情
export const getDocument = (id: number): Promise<Document> => {
  return api.get(`/api/documents/${id}`);
};

// 更新文档
export const updateDocument = (id: number, data: UpdateDocumentRequest): Promise<Document> => {
  return api.put(`/api/documents/${id}`, data);
};

// 删除文档
export const deleteDocument = (id: number): Promise<void> => {
  return api.delete(`/api/documents/${id}`);
};

// 分享文档
export const shareDocument = (data: ShareDocumentRequest): Promise<void> => {
  return api.post('/api/documents/share', data);
};

// 获取文档版本列表
export const getDocumentVersions = (id: number): Promise<any[]> => {
  return api.get(`/api/documents/${id}/versions`);
};
