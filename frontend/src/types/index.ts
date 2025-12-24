// 用户相关类型
export interface User {
  id: number;
  publicId: string;
  username: string;
  email: string;
  avatarUrl?: string;
  profile?: string;
  role: 'USER' | 'ADMIN';
  status: string;
  createdAt?: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  username: string;
  password: string;
  verificationCode: string;
}

export interface LoginResponse {
  token: string;
  user: User;
}

// 文档相关类型
export interface Document {
  id: number;
  title: string;
  ownerId: number;
  owner?: User;
  visibility: 'PUBLIC' | 'PRIVATE';
  docType: string;
  forkedFromId?: number;
  content?: string;
  tags?: string;
  folderId?: number;
  folder?: Folder;
  createdAt: string;
  updatedAt: string;
  isOwner?: boolean;
  canEdit?: boolean;
}

export interface CreateDocumentRequest {
  title: string;
  docType?: string;
  visibility?: 'PUBLIC' | 'PRIVATE';
  folderId?: number;
  tags?: string;
  content?: string;
}

export interface DocumentVersion {
  id: number;
  documentId: number;
  versionNumber: number;
  content: string;
  commitMessage?: string;
  createdBy?: User;
  createdAt: string;
}

// 文件夹相关类型
export interface Folder {
  id: number;
  name: string;
  parentId: number | null;
  children?: Folder[];
}

export interface CreateFolderRequest {
  name: string;
  parentId?: number | null;
}

// 协作者相关类型
export interface Collaborator {
  id: number;
  documentId: number;
  user?: User;
  role: 'EDITOR' | 'VIEWER' | 'OWNER';
  joinedAt: string;
}

export interface WorkspaceRequest {
  id: number;
  user?: User;
  documentId: number;
  message?: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  createdAt: string;
}

// 好友相关类型
export interface Friend {
  id: number;
  user?: User;
  friend?: User;
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED';
  createdAt: string;
}

// 评论相关类型
export interface Comment {
  id: number;
  documentId: number;
  user?: User;
  content: string;
  rangeInfo?: string;
  parentId?: number;
  status: 'OPEN' | 'RESOLVED';
  createdAt: string;
  replies?: Comment[];
}

// 任务相关类型
export interface Task {
  id: number;
  documentId: number;
  title: string;
  description?: string;
  assignee?: User;
  status: 'TODO' | 'IN_PROGRESS' | 'DONE';
  priority: 'LOW' | 'MEDIUM' | 'HIGH';
  dueDate?: string;
  createdAt: string;
}

// 通知相关类型
export interface Notification {
  id: number;
  type: string;
  title: string;
  content: string;
  relatedId?: number;
  isRead: boolean;
  createdAt: string;
}

// 聊天消息类型
export interface ChatMessage {
  id: number;
  documentId: number;
  user?: User;
  content: string;
  createdAt: string;
}

// API响应类型
export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

// WebSocket消息类型
export interface WebSocketMessage {
  type: string;
  payload: any;
  timestamp?: number;
}

export interface CursorPosition {
  userId: number;
  line: number;
  column: number;
}

export interface DocumentOperation {
  type: string;
  content?: string;
  position?: number;
  length?: number;
}
