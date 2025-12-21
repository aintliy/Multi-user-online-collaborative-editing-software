import axiosInstance from '../axios';

export interface Comment {
  id: number;
  documentId: number;
  userId: number;
  parentId?: number;
  content: string;
  rangeInfo?: string;
  status: 'OPEN' | 'RESOLVED';
  createdAt: string;
  updatedAt: string;
}

export interface CommentVO extends Comment {
  username: string;
  userAvatar?: string;
  replies: CommentVO[];
}

export interface CreateCommentRequest {
  documentId: number;
  parentId?: number;
  content: string;
  rangeInfo?: string;
}

/**
 * 创建评论
 */
export const createComment = async (data: CreateCommentRequest): Promise<CommentVO> => {
  const response = await axiosInstance.post('/api/comments', data);
  return response.data.data;
};

/**
 * 获取文档评论列表
 */
export const getCommentsByDocument = async (documentId: number): Promise<CommentVO[]> => {
  const response = await axiosInstance.get(`/api/comments/document/${documentId}`);
  return response.data.data;
};

/**
 * 标记评论为已解决
 */
export const resolveComment = async (id: number): Promise<void> => {
  await axiosInstance.put(`/api/comments/${id}/resolve`);
};

/**
 * 删除评论
 */
export const deleteComment = async (id: number): Promise<void> => {
  await axiosInstance.delete(`/api/comments/${id}`);
};
