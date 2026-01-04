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
  lastLoginAt?: string;
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
  ownerName?: string;
  ownerPublicId?: string;
  ownerAvatarUrl?: string;
  owner?: User;
  visibility: 'PUBLIC' | 'PRIVATE';
  docType: 'markdown' | 'txt';
  forkedFromId?: number;
  content?: string;
  tags?: string;
  folderId?: number;
  folderName?: string;
  createdAt: string;
  updatedAt: string;
  isOwner?: boolean;
  canEdit?: boolean;
  status?: string;
}

export interface CreateDocumentRequest {
  title: string;
  docType?: 'markdown' | 'txt';
  visibility?: 'PUBLIC' | 'PRIVATE';
  folderId?: number;
  tags?: string;
  content?: string;
}

export interface DocumentVersion {
  id: number;
  documentId: number;
  versionNo: number;
  content: string;
  commitMessage?: string;
  createdById?: number;
  createdByName?: string;
  createdAt: string;
}

export interface DocumentCacheResponse {
  confirmedContent?: string;
  userDraftContent?: string;
  onlineUsers?: number[];
  draftTtlSeconds?: number | null;
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
  applicant?: User;
  document?: {
    id: number;
    title: string;
  };
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

// 好友消息类型
export interface FriendMessage {
  id: number;
  sender?: User;
  receiver?: User;
  content: string;
  isRead: boolean;
  createdAt: string;
}

// 评论相关类型
export interface Comment {
  id: number;
  documentId: number;
  userId: number;
  username: string;
  avatarUrl?: string;
  content: string;
  rangeInfo?: string;
  replyToCommentId?: number;
  status: string;
  createdAt: string;
  updatedAt?: string;
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
  items: T[];
  page: number;
  pageSize: number;
  total: number;
}

// WebSocket消息类型
export interface WebSocketMessage {
  type: string;
  data: any;
  documentId?: number;
  userId?: number;
  nickname?: string;
  timestamp?: number;
}

export interface CursorPosition {
  userId: number;
  nickname?: string;
  color?: string;
  line?: number;
  column?: number;
  position?: number;
  isTyping?: boolean;
  lastActivity?: number;
}

export interface DocumentOperation {
  type: string;
  content?: string;
  position?: number;
  length?: number;
  text?: string;
}
