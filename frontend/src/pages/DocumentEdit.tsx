import React, { useEffect, useState, useRef, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Layout,
  Button,
  Space,
  Dropdown,
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
} from '@ant-design/icons';
import Editor from '@monaco-editor/react';
import { documentApi, collaboratorApi, commentApi, chatApi, userApi } from '../api';
import { useAuthStore } from '../store/useAuthStore';
import { useDocumentStore } from '../store/useDocumentStore';
import wsService from '../utils/websocket';
import type { Document, DocumentVersion, Collaborator, Comment, ChatMessage, User } from '../types';
import dayjs from 'dayjs';
import './DocumentEdit.scss';
 
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
  const applyingRemoteRef = useRef(false);
  const joinedRef = useRef(false);
  
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
  
  // Modals
  const [commitModalOpen, setCommitModalOpen] = useState(false);
  const [addCollaboratorModalOpen, setAddCollaboratorModalOpen] = useState(false);
  const [searchUsers, setSearchUsers] = useState<User[]>([]);
  const ttlTimerRef = useRef<number | null>(null);
  
  const [form] = Form.useForm();
  const [collaboratorForm] = Form.useForm();

  const documentId = parseInt(id!);
  const isAdmin = user?.role === 'ADMIN';
  const isPreviewMode = document ? document.canEdit === false : false;

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
    };
  }, [documentId]);

  useEffect(() => {
    if (document && token && !isPreviewMode) {
      wsService.connect(token).then(() => {
        wsService.joinDocument(documentId);
        joinedRef.current = true;
        setupWebSocketHandlers();
      }).catch(console.error);
    }
  }, [document, token, isPreviewMode]);

  const setupWebSocketHandlers = () => {
    wsService.onMessage('JOIN', (msg) => {
      if (msg.data?.onlineUserSummaries) {
        setOnlineUsers(msg.data.onlineUserSummaries);
      }
      if (msg.data?.user) {
        addOnlineUser(msg.data.user);
        message.info(`${msg.data.user.username} 加入了协作`);
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
      }
    });
    
    wsService.onMessage('DRAFT_EDIT', (msg) => {
      if (msg.userId !== user?.id && msg.data?.content) {
        applyRemoteContent(msg.data.content, true);
      }
    });
    
    wsService.onMessage('SAVE_CONFIRMED', (msg) => {
      if (msg.data?.content) {
        applyRemoteContent(msg.data.content, false);
        setIsDirty(false);
        setDirty(false);
        if (msg.userId === user?.id) {
          message.success('已保存到协作缓存');
        } else {
          message.info(`${msg.nickname || '协作者'} 已保存内容`);
        }
      }
    });

    wsService.onMessage('SAVE_REJECTED', (msg) => {
      message.warning(msg.data?.reason || '保存被拒绝，请稍后重试');
    });
    
    wsService.onMessage('CURSOR', (msg) => {
      const targetId = msg.userId ?? msg.data?.userId;
      if (targetId && targetId !== user?.id) {
        updateCursor(targetId, msg.data || {});
      }
    });
    
    wsService.onMessage('CHAT', (msg) => {
      setChatMessages(prev => [...prev, msg.data]);
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
      setComments(data);
    } catch (error) {
      console.error('Failed to fetch comments:', error);
    }
  };

  const fetchChatHistory = async () => {
    try {
      const data = await chatApi.getHistory(documentId);
      setChatMessages(data.items);
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

  const handleEditorMount = (editor: any) => {
    editorRef.current = editor;
    
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

  const handleVisibilityChange = async (value: 'public' | 'private') => {
    if (!document) return;
    setVisibilityUpdating(true);
    try {
      const updated = await documentApi.update(documentId, { visibility: value });
      setDocument(updated);
      setCurrentDocument(updated);
      message.success(value === 'public' ? '已设为公开' : '已设为私有');
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

  const handleAddCollaborator = async (values: any) => {
    try {
      await collaboratorApi.add(documentId, {
        userId: values.userId,
      });
      message.success('添加协作者成功');
      setAddCollaboratorModalOpen(false);
      collaboratorForm.resetFields();
      fetchCollaborators();
    } catch (error: any) {
      message.error(error.response?.data?.message || '添加失败');
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
    if (!newMessage.trim()) return;
    wsService.sendChatMessage(newMessage);
    setNewMessage('');
  };

  const handleAddComment = async (content: string) => {
    try {
      await commentApi.create(documentId, { content });
      message.success('评论成功');
      fetchComments();
    } catch (error: any) {
      message.error(error.response?.data?.message || '评论失败');
    }
  };

  const exportMenuItems = [
    { key: 'pdf', label: 'PDF (.pdf)', onClick: () => window.open(documentApi.exportPdf(documentId)) },
    { key: 'txt', label: '文本 (.txt)', onClick: () => window.open(documentApi.exportTxt(documentId)) },
    { key: 'md', label: 'Markdown (.md)', onClick: () => window.open(documentApi.exportMd(documentId)) },
  ];

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
                value={document.visibility?.toLowerCase() === 'public' ? 'public' : 'private'}
                style={{ width: 120 }}
                onChange={handleVisibilityChange}
                loading={visibilityUpdating}
                options={[
                  { value: 'private', label: '私有' },
                  { value: 'public', label: '公开' },
                ]}
              />
            ) : (
              <Tag color={document.visibility === 'public' ? 'green' : 'default'}>
                {document.visibility === 'public' ? '公开' : '私有'}
              </Tag>
            )
          )}
          {isPreviewMode && <Tag color="volcano">预览模式</Tag>}
          {!isPreviewMode && isDirty && <Tag color="orange">未保存</Tag>}
        </div>
        <div className="header-center">
          <Space>
            {onlineUsers.map(u => (
              <Tooltip key={u.id} title={u.username}>
                <Avatar
                  size="small"
                  src={u.avatarUrl || undefined}
                  icon={<UserOutlined />}
                  style={{ backgroundColor: `hsl(${u.id * 30 % 360}, 70%, 50%)` }}
                />
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
            <Dropdown menu={{ items: exportMenuItems }}>
              <Button icon={<DownloadOutlined />}>导出</Button>
            </Dropdown>
            <Button
              icon={<HistoryOutlined />}
              onClick={() => {
                fetchVersions();
                setHistoryDrawerOpen(true);
              }}
            />
            {!isPreviewMode && (
              <Button
                icon={<TeamOutlined />}
                onClick={() => setCollaboratorsDrawerOpen(true)}
              />
            )}
            <Badge count={comments.length}>
              <Button
                icon={<CommentOutlined />}
                onClick={() => setRightPanelOpen(!rightPanelOpen)}
              />
            </Badge>
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
              onChange={setActiveTab}
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
                              avatar={<Avatar src={comment.user?.avatarUrl || undefined} icon={<UserOutlined />} />}
                              title={comment.user?.username}
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
                          onPressEnter={(e) => {
                            if (!e.shiftKey) {
                              e.preventDefault();
                              handleAddComment((e.target as any).value);
                              (e.target as any).value = '';
                            }
                          }}
                        />
                      </div>
                    </div>
                  ),
                },
                {
                  key: 'chat',
                  label: <span><MessageOutlined /> 聊天</span>,
                  children: (
                    <div className="panel-content chat-panel">
                      <div className="chat-messages">
                        {chatMessages.map((msg, index) => (
                          <div
                            key={index}
                            className={`chat-message ${msg.user?.id === user?.id ? 'own' : ''}`}
                          >
                            <Avatar size="small" src={msg.user?.avatarUrl || undefined} icon={<UserOutlined />} />
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
        width={400}
      >
        <List
          dataSource={versions}
          renderItem={(version) => (
            <List.Item
              actions={[
                <Button size="small" onClick={() => handleRollback(version.id)}>
                  回滚
                </Button>,
              ]}
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
        width={400}
        extra={
          <Button type="primary" onClick={() => setAddCollaboratorModalOpen(true)}>
            添加协作者
          </Button>
        }
      >
        <List
          dataSource={collaborators}
          renderItem={(collaborator) => (
            <List.Item
              actions={[
                collaborator.user?.id !== document?.ownerId && (
                  <Button
                    danger
                    size="small"
                    onClick={() => handleRemoveCollaborator(collaborator.user!.id)}
                  >
                    移除
                  </Button>
                ),
              ]}
            >
              <List.Item.Meta
                avatar={<Avatar src={collaborator.user?.avatarUrl || undefined} icon={<UserOutlined />} />}
                title={
                  <Space>
                    {collaborator.user?.username}
                    {collaborator.user?.id === document?.ownerId && (
                      <Tag color="gold">所有者</Tag>
                    )}
                  </Space>
                }
              />
            </List.Item>
          )}
        />
      </Drawer>

      {/* Add Collaborator Modal */}
      <Modal
        title="添加协作者"
        open={addCollaboratorModalOpen}
        onCancel={() => {
          setAddCollaboratorModalOpen(false);
          collaboratorForm.resetFields();
        }}
        onOk={() => collaboratorForm.submit()}
      >
        <Form form={collaboratorForm} layout="vertical" onFinish={handleAddCollaborator}>
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
                    <Avatar size="small" src={u.avatarUrl || undefined} icon={<UserOutlined />} />
                    {u.username}
                  </Space>
                </Select.Option>
              ))}
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </Layout>
  );
};

export default DocumentEdit;
