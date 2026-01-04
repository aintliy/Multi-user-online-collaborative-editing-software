import React, { useEffect, useState, useRef, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Layout,
  Button,
  Space,
  Modal,
  Form,
  Input,
  Select,
  List,
  Avatar,
  Tag,
  message,
  Tooltip,
  Drawer,
  Badge,
  Tabs,
  Spin,
} from 'antd';
import {
  ArrowLeftOutlined,
  SaveOutlined,
  DownloadOutlined,
  HistoryOutlined,
  TeamOutlined,
  CommentOutlined,
  MessageOutlined,
  UserOutlined,
  InfoCircleOutlined,
} from '@ant-design/icons';
import Editor from '@monaco-editor/react';
import { documentApi, collaboratorApi, commentApi, chatApi, userApi } from '../api';
import { useAuthStore } from '../store/useAuthStore';
import { useDocumentStore } from '../store/useDocumentStore';
import wsService from '../utils/websocket';
import messageBatcher from '../utils/messageBatcher';
import { getAvatarUrl } from '../utils/request';
import type { Document, DocumentVersion, Collaborator, Comment, ChatMessage, User, CursorPosition } from '../types';
import dayjs from 'dayjs';
import './DocumentEdit.scss';

// 用户颜色生成函数
const getUserColor = (userId: number): string => {
  const colors = [
    '#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4', '#FFEAA7',
    '#DDA0DD', '#98D8C8', '#F7DC6F', '#BB8FCE', '#85C1E9'
  ];
  return colors[userId % colors.length];
};


const { Header, Sider, Content } = Layout;
const { TextArea } = Input;

// 映射文档类型到编辑器语言
const getEditorLanguage = (docType: string): string => {
  const languageMap: Record<string, string> = {
    markdown: 'markdown',
    txt: 'plaintext',
  };
  return languageMap[docType] || 'markdown';
};

const DocumentEdit: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user, token } = useAuthStore();
  const { content, setContent, setCurrentDocument, onlineUsers, addOnlineUser, removeOnlineUser, updateCursor, clearOnlineData, setOnlineUsers, setDirty } = useDocumentStore();
  const editorRef = useRef<any>(null);
  const monacoRef = useRef<any>(null);
  const applyingRemoteRef = useRef(false);
  const joinedRef = useRef(false);
  const chatMessagesRef = useRef<HTMLDivElement>(null);
  const cursorDecorationsRef = useRef<string[]>([]);
  const cursorWidgetsRef = useRef<Map<number, any>>(new Map());
  const typingTimeoutRef = useRef<Map<number, ReturnType<typeof setTimeout>>>(new Map());
  
  // 远程用户光标和输入状态
  const [remoteCursors, setRemoteCursors] = useState<Map<number, CursorPosition>>(new Map());
  const [typingUsers, setTypingUsers] = useState<Map<number, string>>(new Map()); // userId -> nickname
  
  const [document, setDocument] = useState<Document | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [rollingBack, setRollingBack] = useState(false);
  const [isDirty, setIsDirty] = useState(false);
  const [draftTtlSeconds, setDraftTtlSeconds] = useState<number | null>(null);
  const [ttlWarningShown, setTtlWarningShown] = useState(false);
  const [visibilityUpdating, setVisibilityUpdating] = useState(false);
  
  // Panels
  const [collaboratorsDrawerOpen, setCollaboratorsDrawerOpen] = useState(false);
  const [historyDrawerOpen, setHistoryDrawerOpen] = useState(false);
  const [rightPanelOpen, setRightPanelOpen] = useState(false);
  const [activeTab, setActiveTab] = useState('comments');
  
  // Data
  const [collaborators, setCollaborators] = useState<Collaborator[]>([]);
  const [versions, setVersions] = useState<DocumentVersion[]>([]);
  const [comments, setComments] = useState<Comment[]>([]);
  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([]);
  const [newMessage, setNewMessage] = useState('');
  const [newComment, setNewComment] = useState('');
  const [unreadChatCount, setUnreadChatCount] = useState(0);
  const [unreadCommentCount, setUnreadCommentCount] = useState(0);
  const sendingRef = useRef(false);
  
  // Modals
  const [commitModalOpen, setCommitModalOpen] = useState(false);
  const [inviteCollaboratorModalOpen, setInviteCollaboratorModalOpen] = useState(false);
  const [collaboratorInfoModalOpen, setCollaboratorInfoModalOpen] = useState(false);
  const [exportModalOpen, setExportModalOpen] = useState(false);
  const [searchUsers, setSearchUsers] = useState<User[]>([]);
  const ttlTimerRef = useRef<number | null>(null);
  
  const [form] = Form.useForm();
  const [collaboratorForm] = Form.useForm();
  const [exportForm] = Form.useForm();

  const documentId = parseInt(id!);
  const isAdmin = user?.role === 'ADMIN';
  const isPreviewMode = document ? document.canEdit === false : false;
  const isOwner = document?.isOwner === true;

  useEffect(() => {
    fetchDocument();
    
    return () => {
      if (joinedRef.current) {
        wsService.leaveDocument();
        joinedRef.current = false;
      }
      clearOnlineData();
      if (ttlTimerRef.current) {
        clearInterval(ttlTimerRef.current);
      }
      // 清除待处理的批量消息
      messageBatcher.clear();
      // 清除所有输入超时定时器
      typingTimeoutRef.current.forEach((timeout) => clearTimeout(timeout));
      typingTimeoutRef.current.clear();
      // 清除光标装饰器
      if (editorRef.current && cursorDecorationsRef.current.length > 0) {
        editorRef.current.deltaDecorations(cursorDecorationsRef.current, []);
        cursorDecorationsRef.current = [];
      }
      // 清除光标 content widgets
      if (editorRef.current) {
        cursorWidgetsRef.current.forEach((widget) => {
          editorRef.current.removeContentWidget(widget);
        });
        cursorWidgetsRef.current.clear();
      }
    };
  }, [documentId]);

  useEffect(() => {
    if (document && token && !isPreviewMode) {
      wsService.connect(token).then(() => {
        wsService.joinDocument(documentId);
        joinedRef.current = true;
        setupWebSocketHandlers();
      }).catch(console.error);
      
      // 清理函数：移除所有消息处理器，防止重复注册
      return () => {
        wsService.offMessage('JOIN');
        wsService.offMessage('ONLINE_USERS');
        wsService.offMessage('LEAVE');
        wsService.offMessage('DRAFT_EDIT');
        wsService.offMessage('SAVE_CONFIRMED');
        wsService.offMessage('SAVE_REJECTED');
        wsService.offMessage('CURSOR');
        wsService.offMessage('CHAT');
      };
    }
  }, [document, token, isPreviewMode]);

  // 聊天消息更新后自动滚动到底部
  useEffect(() => {
    if (chatMessagesRef.current) {
      chatMessagesRef.current.scrollTop = chatMessagesRef.current.scrollHeight;
    }
  }, [chatMessages]);

  const setupWebSocketHandlers = () => {
    wsService.onMessage('JOIN', (msg) => {
      if (msg.data?.onlineUserSummaries) {
        setOnlineUsers(msg.data.onlineUserSummaries);
      }
      if (msg.data?.user) {
        addOnlineUser(msg.data.user);
        // 使用批处理器合并加入消息
        messageBatcher.info(`${msg.data.user.username} 加入了协作`, 'join');
      }
    });

    wsService.onMessage('ONLINE_USERS', (msg) => {
      if (msg.data?.onlineUserSummaries) {
        setOnlineUsers(msg.data.onlineUserSummaries);
      }
    });
    
    wsService.onMessage('LEAVE', (msg) => {
      const targetId = msg.data?.userId ?? msg.userId;
      if (targetId) {
        removeOnlineUser(targetId);
        // 清除该用户的光标和输入状态
        setRemoteCursors(prev => {
          const newMap = new Map(prev);
          newMap.delete(targetId);
          return newMap;
        });
        setTypingUsers(prev => {
          const newMap = new Map(prev);
          newMap.delete(targetId);
          return newMap;
        });
        // 显示离开通知
        if (msg.nickname && targetId !== user?.id) {
          messageBatcher.info(`${msg.nickname} 离开了协作`, 'leave');
        }
      }
    });
    
    wsService.onMessage('DRAFT_EDIT', (msg) => {
      if (msg.userId !== user?.id && msg.data?.content) {
        applyRemoteContent(msg.data.content, true);
        // 标记用户正在输入
        const typingUserId = msg.userId;
        const typingNickname = msg.nickname;
        if (typingUserId && typingNickname) {
          setTypingUsers(prev => {
            const newMap = new Map(prev);
            newMap.set(typingUserId, typingNickname);
            return newMap;
          });
          // 清除之前的超时
          const existingTimeout = typingTimeoutRef.current.get(typingUserId);
          if (existingTimeout) {
            clearTimeout(existingTimeout);
          }
          // 设置新的超时，2秒后清除输入状态
          const timeout = setTimeout(() => {
            setTypingUsers(prev => {
              const newMap = new Map(prev);
              newMap.delete(typingUserId);
              return newMap;
            });
            typingTimeoutRef.current.delete(typingUserId);
          }, 2000);
          typingTimeoutRef.current.set(typingUserId, timeout);
        }
      }
    });
    
    wsService.onMessage('SAVE_CONFIRMED', (msg) => {
      if (msg.data?.content) {
        applyRemoteContent(msg.data.content, false);
        setIsDirty(false);
        setDirty(false);
        if (msg.userId === user?.id) {
          // 使用批处理器合并自己的保存消息
          messageBatcher.success('已保存到协作缓存', 'self-save');
        } else {
          // 使用批处理器合并协作者保存消息
          messageBatcher.info(`${msg.nickname || '协作者'} 已保存内容`, 'save');
        }
      }
    });

    wsService.onMessage('SAVE_REJECTED', (msg) => {
      message.warning(msg.data?.reason || '保存被拒绝，请稍后重试');
    });
    
    wsService.onMessage('CURSOR', (msg) => {
      const targetId = msg.userId ?? msg.data?.userId;
      if (targetId && targetId !== user?.id && msg.data) {
        updateCursor(targetId, msg.data);
        // 更新远程光标状态
        setRemoteCursors(prev => {
          const newMap = new Map(prev);
          newMap.set(targetId, {
            userId: targetId,
            nickname: msg.nickname || msg.data.nickname,
            line: msg.data.line,
            column: msg.data.column,
            color: getUserColor(targetId),
          });
          return newMap;
        });
      }
    });
    
    wsService.onMessage('CHAT', (msg) => {
      // WebSocket返回的数据结构是 { userId, nickname, avatarUrl, content, id }
      // 需要转换成 ChatMessage 格式
      const chatMsg: ChatMessage = {
        id: msg.data.id,
        documentId: documentId,
        content: msg.data.content,
        createdAt: new Date().toISOString(),
        user: {
          id: msg.data.userId,
          username: msg.data.nickname,
          avatarUrl: msg.data.avatarUrl,
        } as User,
      };
      setChatMessages(prev => [...prev, chatMsg]);
      // 如果不是当前用户发送的消息，且聊天面板未打开，增加未读计数
      if (msg.data.userId !== user?.id && (!rightPanelOpen || activeTab !== 'chat')) {
        setUnreadChatCount(prev => prev + 1);
      }
    });
  };

  const fetchDocument = async () => {
    setLoading(true);
    try {
      const doc = await documentApi.getById(documentId);
      setDocument(doc);
      setCurrentDocument(doc);
      setContent(doc.content || '');
      setDirty(false);
      setIsDirty(false);
      const previewMode = doc.canEdit === false;
      
      // Fetch related data
      if (!previewMode) {
        await fetchDocumentCache();
      } else {
        setDraftTtlSeconds(null);
      }
      fetchCollaborators();
      fetchComments();
      fetchChatHistory();
    } catch (error: any) {
      message.error('加载文档失败');
      navigate('/documents');
    } finally {
      setLoading(false);
    }
  };

  const fetchDocumentCache = async () => {
    try {
      const cache = await documentApi.getCache(documentId);
      const nextContent = cache.userDraftContent ?? cache.confirmedContent ?? content;
      setDraftTtlSeconds(cache.draftTtlSeconds ?? null);
      if (nextContent !== undefined) {
        applyRemoteContent(nextContent, false);
      }
    } catch (error) {
      console.error('Failed to fetch document cache:', error);
    }
  };

  // Draft TTL 倒计时与提醒
  useEffect(() => {
    ttlTimerRef.current = setInterval(() => {
      setDraftTtlSeconds((prev) => {
        if (prev === null) return null;
        return Math.max(prev - 1, 0);
      });
    }, 1000);

    return () => {
      if (ttlTimerRef.current) {
        clearInterval(ttlTimerRef.current);
        ttlTimerRef.current = null;
      }
    };
  }, []);

  useEffect(() => {
    if (draftTtlSeconds !== null && draftTtlSeconds <= 180 && !ttlWarningShown) {
      message.warning('草稿缓存即将过期，请及时保存');
      setTtlWarningShown(true);
    }
    if (draftTtlSeconds !== null && draftTtlSeconds > 180 && ttlWarningShown) {
      setTtlWarningShown(false);
    }
  }, [draftTtlSeconds, ttlWarningShown]);

  // 渲染远程用户光标装饰器
  useEffect(() => {
    if (!editorRef.current || !monacoRef.current) return;
    
    const editor = editorRef.current;
    const monaco = monacoRef.current;
    const decorations: any[] = [];
    
    // 移除旧的 content widgets
    cursorWidgetsRef.current.forEach((widget) => {
      editor.removeContentWidget(widget);
    });
    cursorWidgetsRef.current.clear();
    
    remoteCursors.forEach((cursor, remoteUserId) => {
      if (remoteUserId === user?.id) return; // 跳过自己
      if (!cursor.line || !cursor.column) return;
      
      const colorIndex = remoteUserId % 10;
      const isTyping = typingUsers.has(remoteUserId);
      const nickname = cursor.nickname || `用户${remoteUserId}`;
      
      // 光标位置装饰器（竖线）
      decorations.push({
        range: new monaco.Range(cursor.line, cursor.column, cursor.line, cursor.column),
        options: {
          className: `remote-cursor-${colorIndex}`,
          stickiness: monaco.editor.TrackedRangeStickiness.NeverGrowsWhenTypingAtEdges,
        }
      });
      
      // 如果正在输入，高亮整行
      if (isTyping) {
        decorations.push({
          range: new monaco.Range(cursor.line, 1, cursor.line, 1),
          options: {
            isWholeLine: true,
            className: `remote-typing-${colorIndex}`,
            stickiness: monaco.editor.TrackedRangeStickiness.NeverGrowsWhenTypingAtEdges,
          }
        });
      }
      
      // 使用 content widget 显示用户名称标签（解决行末不显示问题）
      const widgetId = `cursor-label-${remoteUserId}`;
      const domNode = window.document.createElement('div');
      domNode.className = `remote-cursor-label-${colorIndex}`;
      domNode.textContent = isTyping ? `${nickname} 正在输入...` : nickname;
      domNode.style.pointerEvents = 'none';
      
      const widget = {
        getId: () => widgetId,
        getDomNode: () => domNode,
        getPosition: () => ({
          position: { lineNumber: cursor.line, column: cursor.column },
          preference: [monaco.editor.ContentWidgetPositionPreference.ABOVE],
        }),
      };
      
      editor.addContentWidget(widget);
      cursorWidgetsRef.current.set(remoteUserId, widget);
    });
    
    // 更新装饰器
    cursorDecorationsRef.current = editor.deltaDecorations(
      cursorDecorationsRef.current,
      decorations
    );
  }, [remoteCursors, typingUsers, user?.id]);

  const fetchCollaborators = async () => {
    try {
      const data = await collaboratorApi.getList(documentId);
      setCollaborators(data);
    } catch (error) {
      console.error('Failed to fetch collaborators:', error);
    }
  };

  const fetchVersions = async () => {
    try {
      const data = await documentApi.getVersions(documentId);
      setVersions(data.items);
    } catch (error) {
      console.error('Failed to fetch versions:', error);
    }
  };

  const fetchComments = async () => {
    try {
      const data = await commentApi.getList(documentId);
      const oldCount = comments.length;
      setComments(data);
      // 如果评论面板未打开，增加未读计数
      if (!rightPanelOpen || activeTab !== 'comments') {
        const newCommentsCount = Math.max(0, data.length - oldCount);
        if (newCommentsCount > 0) {
          setUnreadCommentCount(prev => prev + newCommentsCount);
        }
      }
    } catch (error) {
      console.error('Failed to fetch comments:', error);
    }
  };

  const fetchChatHistory = async () => {
    try {
      const data = await chatApi.getHistory(documentId);
      // 转换后端返回的数据格式为 ChatMessage 格式
      const messages: ChatMessage[] = data.items.map((item: any) => ({
        id: item.id,
        documentId: item.documentId,
        content: item.content,
        createdAt: item.createdAt,
        user: {
          id: item.senderId,
          username: item.senderName,
          avatarUrl: item.avatarUrl,
        } as User,
      }));
      setChatMessages(messages);
    } catch (error) {
      console.error('Failed to fetch chat history:', error);
    }
  };

  const applyRemoteContent = (value: string, markDirty: boolean) => {
    applyingRemoteRef.current = true;

    const model = editorRef.current?.getModel ? editorRef.current.getModel() : null;
    if (model && typeof model.setValue === 'function') {
      model.setValue(value);
    } else if (editorRef.current && typeof editorRef.current.setValue === 'function') {
      editorRef.current.setValue(value);
    }

    setContent(value);
    setDirty(markDirty);
    setIsDirty(markDirty);
    applyingRemoteRef.current = false;
  };

  const handleEditorChange = useCallback((value: string | undefined) => {
    if (isPreviewMode) {
      return;
    }
    if (applyingRemoteRef.current) {
      applyingRemoteRef.current = false;
      return;
    }

    if (value !== undefined) {
      setContent(value);
      setIsDirty(true);
      setDirty(true);
      wsService.sendDraftEdit(value);
    }
  }, [setDirty, isPreviewMode]);

  const handleEditorMount = (editor: any, monaco: any) => {
    editorRef.current = editor;
    monacoRef.current = monaco;
    
    // 注入光标装饰器的CSS样式
    const styleId = 'remote-cursor-styles';
    if (!window.document.getElementById(styleId)) {
      const style = window.document.createElement('style');
      style.id = styleId;
      const colors = [
        '#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4', '#FFEAA7',
        '#DDA0DD', '#98D8C8', '#F7DC6F', '#BB8FCE', '#85C1E9'
      ];
      let cssRules = '';
      colors.forEach((color, index) => {
        cssRules += `
          .remote-cursor-${index} {
            border-left: 2px solid ${color} !important;
            border-right: none !important;
          }
          .remote-cursor-${index}::after {
            content: '';
            position: absolute;
            top: 0;
            left: -2px;
            width: 6px;
            height: 6px;
            background-color: ${color};
            border-radius: 50%;
          }
          .remote-cursor-label-${index} {
            background-color: ${color};
            color: white;
            padding: 2px 8px;
            border-radius: 3px;
            font-size: 11px;
            font-weight: 500;
            white-space: nowrap;
            box-shadow: 0 1px 3px rgba(0,0,0,0.2);
            z-index: 100;
          }
          .remote-typing-${index} {
            background-color: ${color}15 !important;
            border-left: 3px solid ${color} !important;
          }
        `;
      });
      style.textContent = cssRules;
      window.document.head.appendChild(style);
    }
    
    // Track cursor position
    editor.onDidChangeCursorPosition((e: any) => {
      if (!user) return;
      wsService.sendCursor({
        line: e.position.lineNumber,
        column: e.position.column,
        userId: user.id,
      });
    });
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      await documentApi.saveCache(documentId, { content });
      setIsDirty(false);
      setDirty(false);
      message.success('已保存到协作缓存');
    } catch (error: any) {
      message.error(error.response?.data?.message || '保存失败');
    } finally {
      setSaving(false);
    }
  };

  const handleCommit = async (values: { commitMessage: string }) => {
    try {
      await documentApi.commitFromCache(documentId, {
        commitMessage: values.commitMessage,
      });
      message.success('提交成功');
      setIsDirty(false);
      setDirty(false);
      setCommitModalOpen(false);
      form.resetFields();
      fetchVersions();
    } catch (error: any) {
      message.error(error.response?.data?.message || '提交失败');
    }
  };

  const handleRollback = async (versionId: number) => {
    Modal.confirm({
      title: '确认回滚',
      content: '回滚后当前内容将被覆盖，确定要回滚到此版本吗？',
      onOk: async () => {
        try {
          setRollingBack(true);
          await documentApi.rollbackVersion(documentId, versionId);
          message.success('回滚成功');
          fetchDocument();
          setHistoryDrawerOpen(false);
        } catch (error: any) {
          message.error(error.response?.data?.message || '回滚失败');
        } finally {
          setRollingBack(false);
        }
      },
    });
  };

  const handleVisibilityChange = async (value: 'PUBLIC' | 'PRIVATE') => {
    if (!document) return;
    setVisibilityUpdating(true);
    try {
      const updated = await documentApi.update(documentId, { visibility: value });
      setDocument(updated);
      setCurrentDocument(updated);
      message.success(value === 'PUBLIC' ? '已设为公开' : '已设为私有');
    } catch (error: any) {
      message.error(error.response?.data?.message || '更新可见性失败');
    } finally {
      setVisibilityUpdating(false);
    }
  };

  const handleSearchUsers = async (keyword: string) => {
    if (!keyword) {
      setSearchUsers([]);
      return;
    }
    try {
      const users = await userApi.searchUsers(keyword);
      setSearchUsers(users);
    } catch (error) {
      console.error('Failed to search users:', error);
    }
  };

  const handleInviteCollaborator = async (values: any) => {
    try {
      await collaboratorApi.add(documentId, {
        userId: values.userId,
      });
      message.success('邀请已发送，等待对方确认');
      setInviteCollaboratorModalOpen(false);
      collaboratorForm.resetFields();
    } catch (error: any) {
      message.error(error.response?.data?.message || '邀请失败');
    }
  };

  const handleRemoveCollaborator = async (userId: number) => {
    try {
      await collaboratorApi.remove(documentId, userId);
      message.success('移除成功');
      fetchCollaborators();
    } catch (error: any) {
      message.error(error.response?.data?.message || '移除失败');
    }
  };

  const handleSendChatMessage = () => {
    if (!newMessage.trim() || sendingRef.current) return;
    sendingRef.current = true;
    wsService.sendChatMessage(newMessage);
    setNewMessage('');
    // 延迟重置发送状态，防止快速重复点击
    setTimeout(() => {
      sendingRef.current = false;
    }, 300);
  };

  const handleAddComment = async (content: string) => {
    if (!content.trim()) return;
    try {
      await commentApi.create(documentId, { content });
      message.success('评论成功');
      setNewComment('');
      await fetchComments();
    } catch (error: any) {
      message.error(error.response?.data?.message || '评论失败');
    }
  };

  const handleExport = async (values: { filename: string; format: string }) => {
    const { filename, format } = values;
    const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';
    const exportUrl = `${baseUrl}/documents/${documentId}/export/${format}?filename=${encodeURIComponent(filename)}`;
    
    try {
      // 使用 fetch 带认证 token 下载文件
      const response = await fetch(exportUrl, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });
      
      if (!response.ok) {
        throw new Error('导出失败');
      }
      
      // 获取文件内容并创建 Blob
      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      
      // 创建临时链接并触发下载
      const link = window.document.createElement('a');
      link.href = url;
      link.download = `${filename}.${format}`;
      window.document.body.appendChild(link);
      link.click();
      window.document.body.removeChild(link);
      
      // 释放 URL 对象
      window.URL.revokeObjectURL(url);
      
      message.success(`文档导出成功`);
      setExportModalOpen(false);
      exportForm.resetFields();
    } catch (error) {
      message.error('导出失败，请重试');
    }
  };

  const openExportModal = () => {
    // 设置默认文件名为文档标题
    exportForm.setFieldsValue({
      filename: document?.title || '未命名文档',
      format: document?.docType === 'txt' ? 'txt' : 'md',
    });
    setExportModalOpen(true);
  };

  if (loading) {
    return <div className="loading">加载中...</div>;
  }

  return (
    <Layout className="document-edit-page">
      <Header className="edit-header">
        <div className="header-left">
          <Button
            type="text"
            icon={<ArrowLeftOutlined />}
            onClick={() => navigate('/documents')}
          />
          <span className="doc-title">{document?.title}</span>
          {document?.docType && <Tag color="cyan">{document.docType.toUpperCase()}</Tag>}
          {document?.visibility && (
            document?.isOwner ? (
              <Select
                size="small"
                value={document.visibility === 'PUBLIC' ? 'PUBLIC' : 'PRIVATE'}
                style={{ width: 120 }}
                onChange={handleVisibilityChange}
                loading={visibilityUpdating}
                options={[
                  { value: 'PRIVATE', label: '私有' },
                  { value: 'PUBLIC', label: '公开' },
                ]}
              />
            ) : (
              <Tag color={document.visibility === 'PUBLIC' ? 'green' : 'default'}>
                {document.visibility === 'PUBLIC' ? '公开' : '私有'}
              </Tag>
            )
          )}
          {isPreviewMode && <Tag color="volcano">预览模式</Tag>}
          {!isPreviewMode && isDirty && <Tag color="orange">未保存</Tag>}
        </div>
        <div className="header-center">
          <Space>
            {onlineUsers.map(u => (
              <Tooltip 
                key={u.id} 
                title={
                  <span className="user-cursor-indicator">
                    <span 
                      className="cursor-color-dot" 
                      style={{ backgroundColor: getUserColor(u.id) }}
                    />
                    {u.username}
                    {typingUsers.has(u.id) && (
                      <span className="typing-indicator">输入中...</span>
                    )}
                  </span>
                }
              >
                <Badge 
                  dot={typingUsers.has(u.id)} 
                  color={getUserColor(u.id)}
                  offset={[-2, 2]}
                >
                  <Avatar
                    size="small"
                    src={getAvatarUrl(u.avatarUrl)}
                    icon={<UserOutlined />}
                    style={{ 
                      backgroundColor: getUserColor(u.id),
                      boxShadow: typingUsers.has(u.id) ? `0 0 0 2px ${getUserColor(u.id)}` : 'none',
                      transition: 'box-shadow 0.3s ease'
                    }}
                  />
                </Badge>
              </Tooltip>
            ))}
          </Space>
        </div>
        <div className="header-right">
          <Space>
            {!isPreviewMode && (
              <>
                <Button icon={<SaveOutlined />} onClick={handleSave} loading={saving}>
                  {draftTtlSeconds !== null && draftTtlSeconds <= 180
                    ? `保存 (缓存剩余 ${Math.max(Math.ceil(draftTtlSeconds / 60), 0)} 分)`
                    : '保存'}
                </Button>
                <Button onClick={() => setCommitModalOpen(true)}>
                  提交版本
                </Button>
              </>
            )}
            {isPreviewMode ? (
              <Button
                icon={<InfoCircleOutlined />}
                onClick={() => {
                  fetchCollaborators();
                  setCollaboratorInfoModalOpen(true);
                }}
              >
                协作者信息
              </Button>
            ) : (
              <>
                <Button onClick={openExportModal} icon={<DownloadOutlined />}>导出</Button>
                <Button
                  icon={<HistoryOutlined />}
                  onClick={() => {
                    fetchVersions();
                    setHistoryDrawerOpen(true);
                  }}
                />
                <Button
                  icon={<TeamOutlined />}
                  onClick={() => setCollaboratorsDrawerOpen(true)}
                />
                <Badge count={unreadCommentCount}>
                  <Button
                    icon={<CommentOutlined />}
                    onClick={() => {
                      const willOpen = !rightPanelOpen;
                      setRightPanelOpen(willOpen);
                      if (willOpen) {
                        setActiveTab('comments');
                        setUnreadCommentCount(0);
                      }
                    }}
                  />
                </Badge>
              </>
            )}
          </Space>
        </div>
      </Header>
      
      <Layout>
        <Content className="edit-content">
          {isPreviewMode ? (
            <div className="preview-wrapper">
              <div className="preview-banner">
                <Tag color="blue">预览模式</Tag>
                {isAdmin && <Tag color="geekblue">管理员查看</Tag>}
                {!document?.canEdit && <Tag color="gold">无协作权限</Tag>}
              </div>
              <pre className="preview-content">{content}</pre>
            </div>
          ) : (
            <Spin spinning={rollingBack} tip="回滚中...">
              <Editor
                height="100%"
                language={document ? getEditorLanguage(document.docType) : 'markdown'}
                value={content}
                onChange={handleEditorChange}
                onMount={handleEditorMount}
                options={{
                  minimap: { enabled: false },
                  fontSize: 14,
                  lineNumbers: 'on',
                  wordWrap: 'on',
                  automaticLayout: true,
                  readOnly: isPreviewMode,
                }}
              />
            </Spin>
          )}
        </Content>
        
        {!isPreviewMode && rightPanelOpen && (
          <Sider width={360} theme="light" className="right-panel">
            <Tabs
              activeKey={activeTab}
              onChange={(key) => {
                setActiveTab(key);
                if (key === 'chat') {
                  setUnreadChatCount(0);
                } else if (key === 'comments') {
                  setUnreadCommentCount(0);
                }
              }}
              items={[
                {
                  key: 'comments',
                  label: <span><CommentOutlined /> 评论</span>,
                  children: (
                    <div className="panel-content">
                      <List
                        dataSource={comments}
                        renderItem={(comment) => (
                          <List.Item>
                            <List.Item.Meta
                              avatar={<Avatar src={getAvatarUrl(comment.avatarUrl)} icon={<UserOutlined />} />}
                              title={comment.username}
                              description={
                                <>
                                  <div>{comment.content}</div>
                                  <div className="comment-time">
                                    {dayjs(comment.createdAt).format('MM-DD HH:mm')}
                                  </div>
                                </>
                              }
                            />
                          </List.Item>
                        )}
                      />
                      <div className="add-comment">
                        <TextArea
                          placeholder="添加评论..."
                          autoSize={{ minRows: 2, maxRows: 4 }}
                          value={newComment}
                          onChange={(e) => setNewComment(e.target.value)}
                          onPressEnter={(e) => {
                            if (!e.shiftKey) {
                              e.preventDefault();
                              handleAddComment(newComment);
                            }
                          }}
                        />
                        <Button
                          type="primary"
                          size="small"
                          style={{ marginTop: 8 }}
                          onClick={() => handleAddComment(newComment)}
                        >
                          发送评论
                        </Button>
                      </div>
                    </div>
                  ),
                },
                {
                  key: 'chat',
                  label: (
                    <Badge count={unreadChatCount} size="small" offset={[8, 0]}>
                      <span><MessageOutlined /> 聊天</span>
                    </Badge>
                  ),
                  children: (
                    <div className="panel-content chat-panel">
                      <div className="chat-messages" ref={chatMessagesRef}>
                        {chatMessages.map((msg, index) => (
                          <div
                            key={index}
                            className={`chat-message ${msg.user?.id === user?.id ? 'own' : ''}`}
                          >
                            <Avatar size="small" src={getAvatarUrl(msg.user?.avatarUrl)} icon={<UserOutlined />} />
                            <div className="message-content">
                              <div className="message-user">{msg.user?.username}</div>
                              <div className="message-text">{msg.content}</div>
                            </div>
                          </div>
                        ))}
                      </div>
                      <div className="chat-input">
                        <Input.Search
                          placeholder="发送消息..."
                          enterButton="发送"
                          value={newMessage}
                          onChange={(e) => setNewMessage(e.target.value)}
                          onSearch={handleSendChatMessage}
                        />
                      </div>
                    </div>
                  ),
                },
              ]}
            />
          </Sider>
        )}
      </Layout>

      {/* Commit Modal */}
      <Modal
        title="提交版本"
        open={commitModalOpen}
        onCancel={() => setCommitModalOpen(false)}
        onOk={() => form.submit()}
      >
        <Form form={form} layout="vertical" onFinish={handleCommit}>
          <Form.Item
            name="commitMessage"
            label="提交说明"
            rules={[{ required: true, message: '请输入提交说明' }]}
          >
            <TextArea rows={3} placeholder="描述这次修改的内容..." />
          </Form.Item>
        </Form>
      </Modal>

      {/* History Drawer */}
      <Drawer
        title="版本历史"
        open={historyDrawerOpen}
        onClose={() => setHistoryDrawerOpen(false)}
        size={400}
      >
        <List
          dataSource={versions}
          renderItem={(version) => (
            <List.Item
              actions={
                isOwner
                  ? [
                      <Button size="small" onClick={() => handleRollback(version.id)}>
                        回滚
                      </Button>,
                    ]
                  : []
              }
            >
              <List.Item.Meta
                title={version.commitMessage || `版本 ${version.versionNo}`}
                description={
                  <>
                    <div>v{version.versionNo}</div>
                    <div>{dayjs(version.createdAt).format('YYYY-MM-DD HH:mm')}</div>
                    <div>提交者: {version.createdByName || '未知'}</div>
                  </>
                }
              />
            </List.Item>
          )}
        />
      </Drawer>

      {/* Collaborators Drawer */}
      <Drawer
        title="协作者管理"
        open={collaboratorsDrawerOpen}
        onClose={() => setCollaboratorsDrawerOpen(false)}
        size={400}
        extra={
          isOwner && (
            <Button type="primary" onClick={() => setInviteCollaboratorModalOpen(true)}>
              邀请协作者
            </Button>
          )
        }
      >
        <List
          dataSource={collaborators}
          renderItem={(collaborator) => (
            <List.Item
              actions={
                isOwner && collaborator.role !== 'OWNER'
                  ? [
                      <Button
                        danger
                        size="small"
                        onClick={() => handleRemoveCollaborator(collaborator.user!.id)}
                      >
                        移除
                      </Button>,
                    ]
                  : []
              }
            >
              <List.Item.Meta
                avatar={<Avatar src={getAvatarUrl(collaborator.user?.avatarUrl)} icon={<UserOutlined />} />}
                title={
                  <Space>
                    {collaborator.user?.username}
                    {collaborator.role === 'OWNER' && (
                      <Tag color="gold">所有者</Tag>
                    )}
                    {collaborator.role === 'EDITOR' && (
                      <Tag color="blue">协作者</Tag>
                    )}
                  </Space>
                }
              />
            </List.Item>
          )}
        />
      </Drawer>

      {/* Invite Collaborator Modal */}
      <Modal
        title="邀请协作者"
        open={inviteCollaboratorModalOpen}
        onCancel={() => {
          setInviteCollaboratorModalOpen(false);
          collaboratorForm.resetFields();
        }}
        onOk={() => collaboratorForm.submit()}
      >
        <Form form={collaboratorForm} layout="vertical" onFinish={handleInviteCollaborator}>
          <Form.Item
            name="userId"
            label="选择用户"
            rules={[{ required: true, message: '请选择用户' }]}
          >
            <Select
              showSearch
              placeholder="搜索用户..."
              filterOption={false}
              onSearch={handleSearchUsers}
            >
              {searchUsers.map((u) => (
                <Select.Option key={u.id} value={u.id}>
                  <Space>
                    <Avatar size="small" src={getAvatarUrl(u.avatarUrl)} icon={<UserOutlined />} />
                    {u.username}
                  </Space>
                </Select.Option>
              ))}
            </Select>
          </Form.Item>
        </Form>
      </Modal>

      {/* Collaborator Info Modal (for preview mode) */}
      <Modal
        title="协作者信息"
        open={collaboratorInfoModalOpen}
        onCancel={() => setCollaboratorInfoModalOpen(false)}
        footer={[
          <Button key="close" onClick={() => setCollaboratorInfoModalOpen(false)}>
            关闭
          </Button>,
        ]}
      >
        <List
          dataSource={collaborators}
          renderItem={(collaborator) => (
            <List.Item>
              <List.Item.Meta
                avatar={<Avatar src={getAvatarUrl(collaborator.user?.avatarUrl)} icon={<UserOutlined />} />}
                title={
                  <Space>
                    {collaborator.user?.username}
                    {collaborator.role === 'OWNER' && <Tag color="gold">所有者</Tag>}
                    {collaborator.role === 'EDITOR' && <Tag color="blue">协作者</Tag>}
                  </Space>
                }
              />
            </List.Item>
          )}
        />
      </Modal>

      {/* Export Modal */}
      <Modal
        title="导出文档"
        open={exportModalOpen}
        onCancel={() => {
          setExportModalOpen(false);
          exportForm.resetFields();
        }}
        onOk={() => exportForm.submit()}
        okText="导出"
        cancelText="取消"
      >
        <Form
          form={exportForm}
          layout="vertical"
          onFinish={handleExport}
        >
          <Form.Item
            name="filename"
            label="文件名"
            rules={[
              { required: true, message: '请输入文件名' },
              { pattern: /^[^\\/:*?"<>|]+$/, message: '文件名不能包含特殊字符' },
            ]}
          >
            <Input placeholder="请输入导出文件名" />
          </Form.Item>
          <Form.Item
            name="format"
            label="导出格式"
            rules={[{ required: true, message: '请选择导出格式' }]}
          >
            <Select placeholder="请选择导出格式">
              <Select.Option value="md">
                <Space>
                  <span>📝</span>
                  <span>Markdown (.md)</span>
                </Space>
              </Select.Option>
              <Select.Option value="txt">
                <Space>
                  <span>📄</span>
                  <span>纯文本 (.txt)</span>
                </Space>
              </Select.Option>
              <Select.Option value="pdf">
                <Space>
                  <span>📕</span>
                  <span>PDF 文档 (.pdf)</span>
                </Space>
              </Select.Option>
            </Select>
          </Form.Item>
          <div style={{ color: '#999', fontSize: 12, marginTop: 8 }}>
            提示：文件将下载到浏览器默认下载目录
          </div>
        </Form>
      </Modal>
    </Layout>
  );
};

export default DocumentEdit;
