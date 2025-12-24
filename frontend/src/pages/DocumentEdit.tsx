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
} from 'antd';
import {
  ArrowLeftOutlined,
  SaveOutlined,
  DownloadOutlined,
  HistoryOutlined,
  TeamOutlined,
  CommentOutlined,
  CheckSquareOutlined,
  MessageOutlined,
  UserOutlined,
} from '@ant-design/icons';
import Editor from '@monaco-editor/react';
import { documentApi, collaboratorApi, commentApi, taskApi, chatApi, userApi } from '../api';
import { useAuthStore } from '../store/useAuthStore';
import { useDocumentStore } from '../store/useDocumentStore';
import wsService from '../utils/websocket';
import type { Document, DocumentVersion, Collaborator, Comment, Task, ChatMessage, User } from '../types';
import dayjs from 'dayjs';
import './DocumentEdit.scss';

const { Header, Sider, Content } = Layout;
const { TextArea } = Input;

const DocumentEdit: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user, token } = useAuthStore();
  const { content, setContent, setCurrentDocument, onlineUsers, addOnlineUser, removeOnlineUser, updateCursor, clearOnlineData } = useDocumentStore();
  const editorRef = useRef<any>(null);
  
  const [document, setDocument] = useState<Document | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [isDirty, setIsDirty] = useState(false);
  
  // Panels
  const [collaboratorsDrawerOpen, setCollaboratorsDrawerOpen] = useState(false);
  const [historyDrawerOpen, setHistoryDrawerOpen] = useState(false);
  const [rightPanelOpen, setRightPanelOpen] = useState(false);
  const [activeTab, setActiveTab] = useState('comments');
  
  // Data
  const [collaborators, setCollaborators] = useState<Collaborator[]>([]);
  const [versions, setVersions] = useState<DocumentVersion[]>([]);
  const [comments, setComments] = useState<Comment[]>([]);
  const [tasks, setTasks] = useState<Task[]>([]);
  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([]);
  const [newMessage, setNewMessage] = useState('');
  
  // Modals
  const [commitModalOpen, setCommitModalOpen] = useState(false);
  const [addCollaboratorModalOpen, setAddCollaboratorModalOpen] = useState(false);
  const [searchUsers, setSearchUsers] = useState<User[]>([]);
  
  const [form] = Form.useForm();
  const [collaboratorForm] = Form.useForm();

  const documentId = parseInt(id!);

  useEffect(() => {
    fetchDocument();
    
    return () => {
      wsService.leaveDocument();
      clearOnlineData();
    };
  }, [documentId]);

  useEffect(() => {
    if (document && token) {
      // Connect WebSocket and join document
      wsService.connect(token).then(() => {
        wsService.joinDocument(documentId);
        setupWebSocketHandlers();
      }).catch(console.error);
    }
  }, [document, token]);

  const setupWebSocketHandlers = () => {
    wsService.onMessage('USER_JOINED', (msg) => {
      addOnlineUser(msg.payload.user);
      message.info(`${msg.payload.user.username} 加入了协作`);
    });
    
    wsService.onMessage('USER_LEFT', (msg) => {
      removeOnlineUser(msg.payload.userId);
    });
    
    wsService.onMessage('EDIT', (msg) => {
      if (msg.payload.userId !== user?.id) {
        handleRemoteEdit(msg.payload);
      }
    });
    
    wsService.onMessage('CURSOR', (msg) => {
      if (msg.payload.userId !== user?.id) {
        updateCursor(msg.payload.userId, msg.payload);
      }
    });
    
    wsService.onMessage('CHAT', (msg) => {
      setChatMessages(prev => [...prev, msg.payload]);
    });
  };

  const fetchDocument = async () => {
    setLoading(true);
    try {
      const doc = await documentApi.getById(documentId);
      setDocument(doc);
      setCurrentDocument(doc);
      setContent(doc.content || '');
      
      // Fetch related data
      fetchCollaborators();
      fetchComments();
      fetchTasks();
      fetchChatHistory();
    } catch (error: any) {
      message.error('加载文档失败');
      navigate('/documents');
    } finally {
      setLoading(false);
    }
  };

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
      setVersions(data.content);
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

  const fetchTasks = async () => {
    try {
      const data = await taskApi.getList(documentId);
      setTasks(data);
    } catch (error) {
      console.error('Failed to fetch tasks:', error);
    }
  };

  const fetchChatHistory = async () => {
    try {
      const data = await chatApi.getHistory(documentId);
      setChatMessages(data.content);
    } catch (error) {
      console.error('Failed to fetch chat history:', error);
    }
  };

  const handleRemoteEdit = (_operation: any) => {
    // Apply remote edit to editor
    // This is a simplified version - real OT would be more complex
    if (editorRef.current) {
      // Apply the operation to editor
      // For now, just refresh the content from server
    }
  };

  const handleEditorChange = useCallback((value: string | undefined) => {
    if (value !== undefined) {
      setContent(value);
      setIsDirty(true);
      
      // Send edit to other users
      wsService.sendEdit({
        type: 'replace',
        content: value,
      });
    }
  }, []);

  const handleEditorMount = (editor: any) => {
    editorRef.current = editor;
    
    // Track cursor position
    editor.onDidChangeCursorPosition((e: any) => {
      wsService.sendCursor({
        line: e.position.lineNumber,
        column: e.position.column,
        userId: user!.id,
      });
    });
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      await documentApi.update(documentId, { content });
      setIsDirty(false);
      message.success('保存成功');
    } catch (error: any) {
      message.error(error.response?.data?.message || '保存失败');
    } finally {
      setSaving(false);
    }
  };

  const handleCommit = async (values: { commitMessage: string }) => {
    try {
      await documentApi.commit(documentId, {
        content,
        commitMessage: values.commitMessage,
      });
      message.success('提交成功');
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
          await documentApi.rollbackVersion(documentId, versionId);
          message.success('回滚成功');
          fetchDocument();
          setHistoryDrawerOpen(false);
        } catch (error: any) {
          message.error(error.response?.data?.message || '回滚失败');
        }
      },
    });
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
        role: values.role,
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

  const handleAddTask = async (title: string) => {
    try {
      await taskApi.create(documentId, { title });
      message.success('任务创建成功');
      fetchTasks();
    } catch (error: any) {
      message.error(error.response?.data?.message || '创建失败');
    }
  };

  const handleToggleTask = async (task: Task) => {
    try {
      await taskApi.update(task.id, {
        status: task.status === 'DONE' ? 'TODO' : 'DONE',
      });
      fetchTasks();
    } catch (error: any) {
      message.error(error.response?.data?.message || '更新失败');
    }
  };

  const exportMenuItems = [
    { key: 'word', label: 'Word (.docx)', onClick: () => window.open(documentApi.exportWord(documentId)) },
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
          {isDirty && <Tag color="orange">未保存</Tag>}
        </div>
        <div className="header-center">
          <Space>
            {onlineUsers.map(u => (
              <Tooltip key={u.id} title={u.username}>
                <Avatar
                  size="small"
                  src={u.avatarUrl}
                  icon={<UserOutlined />}
                  style={{ backgroundColor: `hsl(${u.id * 30 % 360}, 70%, 50%)` }}
                />
              </Tooltip>
            ))}
          </Space>
        </div>
        <div className="header-right">
          <Space>
            <Button icon={<SaveOutlined />} onClick={handleSave} loading={saving}>
              保存
            </Button>
            <Button onClick={() => setCommitModalOpen(true)}>
              提交版本
            </Button>
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
            <Button
              icon={<TeamOutlined />}
              onClick={() => setCollaboratorsDrawerOpen(true)}
            />
            <Badge count={comments.length + tasks.length}>
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
          <Editor
            height="100%"
            defaultLanguage="markdown"
            value={content}
            onChange={handleEditorChange}
            onMount={handleEditorMount}
            options={{
              minimap: { enabled: false },
              fontSize: 14,
              lineNumbers: 'on',
              wordWrap: 'on',
              automaticLayout: true,
            }}
          />
        </Content>
        
        {rightPanelOpen && (
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
                              avatar={<Avatar src={comment.user?.avatarUrl} icon={<UserOutlined />} />}
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
                  key: 'tasks',
                  label: <span><CheckSquareOutlined /> 任务</span>,
                  children: (
                    <div className="panel-content">
                      <List
                        dataSource={tasks}
                        renderItem={(task) => (
                          <List.Item
                            actions={[
                              <Button
                                type="text"
                                size="small"
                                onClick={() => handleToggleTask(task)}
                              >
                                {task.status === 'DONE' ? '标记未完成' : '标记完成'}
                              </Button>,
                            ]}
                          >
                            <List.Item.Meta
                              title={
                                <span style={{ textDecoration: task.status === 'DONE' ? 'line-through' : 'none' }}>
                                  {task.title}
                                </span>
                              }
                              description={task.assignee?.username}
                            />
                          </List.Item>
                        )}
                      />
                      <div className="add-task">
                        <Input
                          placeholder="添加任务..."
                          onPressEnter={(e) => {
                            handleAddTask((e.target as any).value);
                            (e.target as any).value = '';
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
                            <Avatar size="small" src={msg.user?.avatarUrl} icon={<UserOutlined />} />
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
                title={version.commitMessage || `版本 ${version.versionNumber}`}
                description={
                  <>
                    <div>v{version.versionNumber}</div>
                    <div>{dayjs(version.createdAt).format('YYYY-MM-DD HH:mm')}</div>
                    <div>提交者: {version.createdBy?.username}</div>
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
                collaborator.user?.id !== document?.owner?.id && (
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
                avatar={<Avatar src={collaborator.user?.avatarUrl} icon={<UserOutlined />} />}
                title={
                  <Space>
                    {collaborator.user?.username}
                    {collaborator.user?.id === document?.owner?.id && (
                      <Tag color="gold">所有者</Tag>
                    )}
                  </Space>
                }
                description={collaborator.role}
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
                    <Avatar size="small" src={u.avatarUrl} icon={<UserOutlined />} />
                    {u.username}
                  </Space>
                </Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="role" label="角色" initialValue="EDITOR">
            <Select>
              <Select.Option value="VIEWER">查看者</Select.Option>
              <Select.Option value="EDITOR">编辑者</Select.Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </Layout>
  );
};

export default DocumentEdit;
