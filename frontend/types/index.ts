export type UserRole = "USER" | "ADMIN";
export type DocumentVisibility = "public" | "private";
export type TaskStatus = "TODO" | "DOING" | "DONE";
export type TaskPriority = "LOW" | "MEDIUM" | "HIGH";
export type CollaboratorRole = "VIEWER" | "EDITOR";

export interface PageResponse<T> {
  items: T[];
  page: number;
  pageSize: number;
  total: number;
}

export interface User {
  id: number;
  publicId: string;
  username: string;
  email: string;
  phone?: string;
  avatarUrl?: string;
  profile?: string;
  role: UserRole;
  status?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface LoginResponse {
  token: string;
  user: User;
}

export interface Document {
  id: number;
  title: string;
  ownerId: number;
  ownerName?: string;
  content?: string;
  docType: string;
  visibility: DocumentVisibility;
  tags?: string;
  status?: string;
  forkedFromId?: number;
  createdAt?: string;
  updatedAt?: string;
  isOwner?: boolean;
  canEdit?: boolean;
}

export interface DocumentVersion {
  id: number;
  documentId: number;
  versionNo: number;
  content: string;
  commitMessage?: string;
  createdBy: number;
  creatorName?: string;
  createdAt?: string;
}

export interface Collaborator {
  id: number;
  documentId: number;
  userId: number;
  username: string;
  role: CollaboratorRole;
  avatarUrl?: string;
}

export interface Comment {
  id: number;
  documentId: number;
  parentId?: number;
  authorId: number;
  authorName?: string;
  content: string;
  status?: string;
  createdAt?: string;
  replies?: Comment[];
}

export interface Task {
  id: number;
  documentId?: number;
  documentTitle?: string;
  creatorId: number;
  creatorName?: string;
  assigneeId?: number;
  assigneeName?: string;
  title: string;
  description?: string;
  status: TaskStatus;
  priority: TaskPriority;
  dueDate?: string;
  createdAt?: string;
}

export interface Notification {
  id: number;
  type: string;
  title: string;
  content: string;
  read: boolean;
  createdAt: string;
  extra?: Record<string, unknown>;
}

export interface DocumentFilters {
  keyword?: string;
  tag?: string;
  ownerId?: number;
}

export interface EditOperation {
  operation: "insert" | "delete" | "replace";
  index: number;
  length?: number;
  text?: string;
}

export interface CursorPosition {
  index: number;
  length?: number;
  color?: string;
}

export interface OnlineUser {
  userId: number;
  username: string;
  color: string;
  avatarUrl?: string;
}

export interface ChatMessage {
  id: string;
  userId: number;
  username: string;
  content: string;
  timestamp: number;
  type: "SYSTEM" | "CHAT";
}
