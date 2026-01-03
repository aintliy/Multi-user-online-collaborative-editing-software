import request, { get, post, put, del, upload } from '../utils/request';
import type {
  User,
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  Document,
  DocumentCacheResponse,
  CreateDocumentRequest,
  DocumentVersion,
  Folder,
  CreateFolderRequest,
  Collaborator,
  WorkspaceRequest,
  Friend,
  FriendMessage,
  Comment,
  Notification,
  ChatMessage,
  PageResponse,
} from '../types';

// ========== 认证相关 API ==========
export const authApi = {
  // 发送验证码
  sendVerificationCode: (email: string) =>
    post<void>('/auth/send-verification-code', { email }),
  
  // 注册
  register: (data: RegisterRequest) =>
    post<User>('/auth/register', data),
  
  // 登录
  login: (data: LoginRequest) =>
    post<LoginResponse>('/auth/login', data),
  
  // 获取当前用户
  getCurrentUser: () =>
    get<User>('/auth/me'),
  
  // 更新个人资料
  updateProfile: (data: { username?: string; profile?: string }) =>
    put<User>('/auth/profile', data),
  
  // 忘记密码
  forgotPassword: (email: string) =>
    post<void>('/auth/forgot-password', { email }),
  
  // 重置密码
  resetPassword: (token: string, newPassword: string) =>
    post<void>('/auth/reset-password', { token, newPassword }),
};

// ========== 用户相关 API ==========
export const userApi = {
  // 搜索用户
  searchUsers: (keyword: string) =>
    get<User[]>('/users/search', { params: { keyword } }),
  
  // 上传头像
  uploadAvatar: (file: File) =>
    upload<{ avatarUrl: string }>('/users/me/avatar', file),
  
  // 获取用户公开仓库
  getUserRepos: (publicId: string) =>
    get<User>(`/users/${publicId}/repos`),
  
  // 获取用户公开文档列表
  getUserPublicDocs: (publicId: string) =>
    get<{ user: User; documents: Document[] }>(`/users/${publicId}/public-documents`),
};

// ========== 文档相关 API ==========
export const documentApi = {
  // 创建文档
  create: (data: CreateDocumentRequest) =>
    post<Document>('/documents', {
      ...data,
      visibility: normalizeVisibility(data.visibility),
    }),
  
  // 获取文档详情
  getById: (id: number) =>
    get<Document>(`/documents/${id}`),

  // 获取协作缓存态
  getCache: (id: number) =>
    get<DocumentCacheResponse>(`/documents/${id}/cache`),

  // 保存确认态到 Redis
  saveCache: (id: number, data: { content: string }) =>
    post<void>(`/documents/${id}/cache/save`, data),
  
  // 更新文档
  update: (id: number, data: Partial<CreateDocumentRequest>) =>
    put<Document>(`/documents/${id}`, {
      ...data,
      visibility: normalizeVisibility(data.visibility),
    }),
  
  // 删除文档
  delete: (id: number) =>
    del<void>(`/documents/${id}`),
  
  // 获取文档列表
  getList: (params?: { folderId?: number; keyword?: string; page?: number; pageSize?: number }) =>
    get<PageResponse<Document>>('/documents', { params }),
  
  // 搜索公开文档
  searchPublic: (params?: { keyword?: string; page?: number; pageSize?: number }) =>
    get<PageResponse<Document>>('/documents/public', { params }),
  
  // 获取用户已加入的协作文档
  getCollaborating: () =>
    get<Document[]>('/documents/collaborating'),
  
  // 提交文档版本
  commit: (id: number, data: { content: string; commitMessage: string }) =>
    post<DocumentVersion>(`/documents/${id}/commits`, data),

  // 从缓存提交版本
  commitFromCache: (id: number, data: { commitMessage: string }) =>
    post<DocumentVersion>(`/documents/${id}/commits/from-cache`, data),
  
  // 获取版本列表
  getVersions: (id: number, params?: { page?: number; pageSize?: number }) =>
    get<PageResponse<DocumentVersion>>(`/documents/${id}/versions`, { params }),
  
  // 获取版本详情
  getVersion: (docId: number, versionId: number) =>
    get<DocumentVersion>(`/documents/${docId}/versions/${versionId}`),
  
  // 回滚版本
  rollbackVersion: (docId: number, versionId: number) =>
    post<DocumentVersion>(`/documents/${docId}/versions/${versionId}/rollback`),
  
  // 克隆文档
  clone: (id: number, data?: { title?: string; folderId?: number }) =>
    post<Document>(`/documents/${id}/clone`, data),

  // 导入文档
  import: (file: File, folderId?: number | null) => {
    const formData = new FormData();
    formData.append('file', file);
    if (folderId !== undefined && folderId !== null) {
      formData.append('folderId', String(folderId));
    }
    return request.post('/documents/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then(res => res.data.data as Document);
  },
  
  // 移动文档
  move: (id: number, folderId: number | null) =>
    put<Document>(`/documents/${id}/move`, { folderId }),
  
  // 导出文档
  exportPdf: (id: number) => `/documents/${id}/export/pdf`,
  exportTxt: (id: number) => `/documents/${id}/export/txt`,
  exportMd: (id: number) => `/documents/${id}/export/md`,
};

// ========== 文件夹相关 API ==========
export const folderApi = {
  // 获取文件夹树
  getTree: () =>
    get<Folder[]>('/folders'),
  
  // 创建文件夹
  create: (data: CreateFolderRequest) =>
    post<Folder>('/folders', data),
  
  // 更新文件夹
  update: (id: number, data: { name: string }) =>
    put<Folder>(`/folders/${id}`, data),
  
  // 删除文件夹
  delete: (id: number) =>
    del<void>(`/folders/${id}`),
};

// ========== 协作相关 API ==========
export const collaboratorApi = {
  // 获取协作者列表
  getList: (documentId: number) =>
    get<Collaborator[]>(`/documents/${documentId}/collaborators`),
  
  // 添加协作者
  add: (documentId: number, data: { userId: number; role?: string }) =>
    post<Collaborator>(`/documents/${documentId}/collaborators`, data),
  
  // 移除协作者
  remove: (documentId: number, userId: number) =>
    del<void>(`/documents/${documentId}/collaborators/${userId}`),
  
  // 获取当前用户所有文档的待处理协作申请
  getMyPendingRequests: () =>
    get<WorkspaceRequest[]>('/workspace-requests/pending'),
  
  // 提交协作申请
  submitRequest: (documentId: number, data?: { message?: string }) =>
    post<void>(`/documents/${documentId}/workspace-requests`, data),
  
  // 获取协作申请列表
  getRequests: (documentId: number) =>
    get<WorkspaceRequest[]>(`/documents/${documentId}/workspace-requests`),
  
  // 审批通过
  approveRequest: (documentId: number, requestId: number) =>
    post<void>(`/documents/${documentId}/workspace-requests/${requestId}/approve`),
  
  // 审批拒绝
  rejectRequest: (documentId: number, requestId: number) =>
    post<void>(`/documents/${documentId}/workspace-requests/${requestId}/reject`),
  
  // 获取当前用户收到的待处理协作邀请
  getMyPendingInvites: () =>
    get<WorkspaceRequest[]>('/collaborator-invites/pending'),
  
  // 接受协作邀请
  acceptInvite: (inviteId: number) =>
    post<void>(`/collaborator-invites/${inviteId}/accept`),
  
  // 拒绝协作邀请
  rejectInvite: (inviteId: number) =>
    post<void>(`/collaborator-invites/${inviteId}/reject`),
};

// ========== 好友相关 API ==========
export const friendApi = {
  // 发送好友请求
  sendRequest: (userId: number, message?: string) =>
    post<void>('/friends/requests', { userId, message }),
  
  // 获取好友请求列表
  getRequests: () =>
    get<Friend[]>('/friends/requests'),
  
  // 接受好友请求
  acceptRequest: (requestId: number) =>
    post<void>(`/friends/requests/${requestId}/accept`),
  
  // 拒绝好友请求
  rejectRequest: (requestId: number) =>
    post<void>(`/friends/requests/${requestId}/reject`),
  
  // 获取好友列表
  getList: () =>
    get<Friend[]>('/friends'),
  
  // 删除好友
  delete: (friendUserId: number) =>
    del<void>(`/friends/${friendUserId}`),
  
  // 发送消息给好友
  sendMessage: (receiverId: number, content: string) =>
    post<FriendMessage>('/friends/messages', { receiverId, content }),
  
  // 获取与某好友的聊天记录
  getMessages: (friendId: number) =>
    get<FriendMessage[]>(`/friends/${friendId}/messages`),
  
  // 分页获取聊天记录
  getMessagesPaged: (friendId: number, page?: number, pageSize?: number) =>
    get<PageResponse<FriendMessage>>(`/friends/${friendId}/messages/paged`, { params: { page, pageSize } }),
  
  // 标记消息为已读
  markAsRead: (friendId: number) =>
    post<void>(`/friends/${friendId}/messages/read`),
  
  // 获取未读消息数量
  getUnreadCount: () =>
    get<{ count: number }>('/friends/messages/unread-count'),
};

// ========== 评论相关 API ==========
export const commentApi = {
  // 创建评论
  create: (documentId: number, data: { content: string; rangeInfo?: string; parentId?: number }) =>
    post<Comment>(`/documents/${documentId}/comments`, data),
  
  // 获取评论列表
  getList: (documentId: number) =>
    get<Comment[]>(`/documents/${documentId}/comments`),
  
  // 更新评论状态
  update: (commentId: number, data: { status?: string }) =>
    put<Comment>(`/comments/${commentId}`, data),
};

// ========== 通知相关 API ==========
export const notificationApi = {
  // 获取通知列表
  getList: (params?: { isRead?: boolean; page?: number; pageSize?: number }) =>
    get<PageResponse<Notification>>('/notifications', { params }),
  
  // 标记已读
  markAsRead: (id: number) =>
    post<void>(`/notifications/${id}/read`),
  
  // 获取未读数量
  getUnreadCount: () =>
    get<{ count: number }>('/notifications/unread-count'),
};

// ========== 聊天相关 API ==========
export const chatApi = {
  // 获取聊天历史
  getHistory: (documentId: number, params?: { page?: number; pageSize?: number }) =>
    get<PageResponse<ChatMessage>>(`/documents/${documentId}/chat-messages`, { params }),
};

// ========== 管理员相关 API ==========
export const adminApi = {
  // 获取系统统计
  getStats: () =>
    get<any>('/admin/stats'),
  
  // 获取用户列表
  getUsers: (params?: { keyword?: string; status?: string; page?: number; pageSize?: number }) =>
    get<PageResponse<User>>('/admin/users', { params }),
  
  // 禁用用户
  banUser: (userId: number) =>
    post<void>(`/admin/users/${userId}/ban`),
  
  // 解禁用户
  unbanUser: (userId: number) =>
    post<void>(`/admin/users/${userId}/unban`),
  
  // 删除用户
  deleteUser: (userId: number) =>
    del<void>(`/admin/users/${userId}`),
  
  // 获取文档列表
  getDocuments: (params?: { keyword?: string; page?: number; pageSize?: number }) =>
    get<PageResponse<Document>>('/admin/documents', { params }),
  
  // 删除文档
  deleteDocument: (documentId: number) =>
    del<void>(`/admin/documents/${documentId}`),
  
  // 获取操作日志
  getOperationLogs: (params?: { userId?: number; operationType?: string; page?: number; pageSize?: number }) =>
    get<PageResponse<any>>('/admin/operation-logs', { params }),
};

function normalizeVisibility(value?: string | null) {
  if (!value) return value;
  const upper = value.toUpperCase();
  return upper === 'PUBLIC' || upper === 'PRIVATE' ? upper : value;
}
