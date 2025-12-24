import { create } from 'zustand';
import type { Document, CursorPosition } from '../types';

interface OnlineUser {
  id: number;
  username: string;
  avatarUrl?: string;
}

interface DocumentState {
  currentDocument: Document | null;
  content: string;
  isDirty: boolean;
  onlineUsers: OnlineUser[];
  cursors: Map<number, CursorPosition>;
  setCurrentDocument: (doc: Document | null) => void;
  setContent: (content: string) => void;
  setDirty: (dirty: boolean) => void;
  setOnlineUsers: (users: OnlineUser[]) => void;
  addOnlineUser: (user: OnlineUser) => void;
  removeOnlineUser: (userId: number) => void;
  updateCursor: (userId: number, cursor: CursorPosition) => void;
  removeCursor: (userId: number) => void;
  clearCursors: () => void;
  clearOnlineData: () => void;
}

export const useDocumentStore = create<DocumentState>((set) => ({
  currentDocument: null,
  content: '',
  isDirty: false,
  onlineUsers: [],
  cursors: new Map(),
  setCurrentDocument: (doc) => set({ currentDocument: doc, content: doc?.content || '' }),
  setContent: (content) => set({ content, isDirty: true }),
  setDirty: (dirty) => set({ isDirty: dirty }),
  setOnlineUsers: (users) => set({ onlineUsers: users }),
  addOnlineUser: (user) =>
    set((state) => ({
      onlineUsers: state.onlineUsers.some(u => u.id === user.id)
        ? state.onlineUsers
        : [...state.onlineUsers, user]
    })),
  removeOnlineUser: (userId) =>
    set((state) => ({
      onlineUsers: state.onlineUsers.filter(u => u.id !== userId)
    })),
  updateCursor: (userId, cursor) =>
    set((state) => {
      const newCursors = new Map(state.cursors);
      newCursors.set(userId, cursor);
      return { cursors: newCursors };
    }),
  removeCursor: (userId) =>
    set((state) => {
      const newCursors = new Map(state.cursors);
      newCursors.delete(userId);
      return { cursors: newCursors };
    }),
  clearCursors: () => set({ cursors: new Map() }),
  clearOnlineData: () => set({ onlineUsers: [], cursors: new Map() }),
}));
