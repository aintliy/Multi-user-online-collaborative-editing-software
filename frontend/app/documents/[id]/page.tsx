'use client';

import React, { useEffect, useState, useRef, useCallback } from 'react';
import { useParams, useRouter } from 'next/navigation';
import dynamic from 'next/dynamic';
import {
  Layout,
  Typography,
  Space,
  Avatar,
  Tag,
  message,
  Spin,
  Button,
  Drawer,
  List,
  Input,
  Badge,
} from 'antd';
import {
  SaveOutlined,
  UsergroupAddOutlined,
  CommentOutlined,
  ArrowLeftOutlined,
} from '@ant-design/icons';
import { useAuth } from '@/contexts/AuthContext';
import { getDocumentById, updateDocument, Document } from '@/lib/api/document';
import { getCommentsByDocument, createComment, CommentVO } from '@/lib/api/comment';
import {
  websocketClient,
  OnlineUser,
  EditOperation,
  WebSocketMessage,
} from '@/lib/websocket';
import 'react-quill/dist/quill.snow.css';

// 动态导入Quill编辑器（避免SSR问题）
const ReactQuill = dynamic(() => import('react-quill'), { ssr: false });

const { Header, Content, Sider } = Layout;
const { Title, Text } = Typography;
const { TextArea } = Input;

export default function DocumentEditPage() {
  const params = useParams();
  const router = useRouter();
  const { user } = useAuth();
  const docId = Number(params.id);

  const [document, setDocument] = useState<Document | null>(null);
  const [content, setContent] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [onlineUsers, setOnlineUsers] = useState<OnlineUser[]>([]);
  const [comments, setComments] = useState<CommentVO[]>([]);
  const [commentDrawerVisible, setCommentDrawerVisible] = useState(false);
  const [newComment, setNewComment] = useState('');

  const quillRef = useRef<any>(null);
  const autoSaveTimerRef = useRef<NodeJS.Timeout | null>(null);
  const contentRef = useRef(content);

  // 更新contentRef
  useEffect(() => {
    contentRef.current = content;
  }, [content]);

  // 加载文档
  useEffect(() => {
    fetchDocument();
    fetchComments();
  }, [docId]);

  // WebSocket连接
  useEffect(() => {
    if (!user) return;

    const token = localStorage.getItem('token');
    if (!token) return;

    // 连接WebSocket
    const connectWS = async () => {
      try {
        if (!websocketClient.isConnected()) {
          await websocketClient.connect(token);
        }
        
        // 订阅文档频道
        websocketClient.subscribeDocument(docId, handleWebSocketMessage);
        
        // 请求在线用户列表
        websocketClient.requestOnlineUsers(docId);
      } catch (error) {
        console.error('WebSocket connection failed:', error);
      }
    };

    connectWS();

    return () => {
      websocketClient.unsubscribeDocument(docId);
    };
  }, [user, docId]);

  // 自动保存
  useEffect(() => {
    autoSaveTimerRef.current = setInterval(() => {
      if (contentRef.current !== document?.content) {
        handleSave();
      }
    }, 10000); // 每10秒自动保存

    return () => {
      if (autoSaveTimerRef.current) {
        clearInterval(autoSaveTimerRef.current);
      }
    };
  }, [document]);

  const fetchDocument = async () => {
    setLoading(true);
    try {
      const doc = await getDocumentById(docId);
      setDocument(doc);
      setContent(doc.content);
    } catch (error: any) {
      message.error('获取文档失败');
      router.push('/documents');
    } finally {
      setLoading(false);
    }
  };

  const fetchComments = async () => {
    try {
      const data = await getCommentsByDocument(docId);
      setComments(data);
    } catch (error) {
      console.error('Failed to fetch comments:', error);
    }
  };

  const handleWebSocketMessage = useCallback((msg: WebSocketMessage) => {
    switch (msg.type) {
      case 'JOIN':
        setOnlineUsers(msg.payload as OnlineUser[]);
        message.info(`${msg.username} 加入了文档`);
        break;
      case 'LEAVE':
        setOnlineUsers(msg.payload as OnlineUser[]);
        message.info(`${msg.username} 离开了文档`);
        break;
      case 'EDIT':
        // 应用远程编辑操作
        applyRemoteEdit(msg.payload as EditOperation);
        break;
      case 'ONLINE_USERS':
        setOnlineUsers(msg.payload as OnlineUser[]);
        break;
      case 'COMMENT':
        // 刷新评论
        fetchComments();
        break;
      default:
        break;
    }
  }, []);

  const applyRemoteEdit = (operation: EditOperation) => {
    const quill = quillRef.current?.getEditor();
    if (!quill) return;

    // 应用远程操作到本地编辑器
    switch (operation.operation) {
      case 'INSERT':
        quill.insertText(operation.position, operation.content || '');
        break;
      case 'DELETE':
        quill.deleteText(operation.position, operation.length || 0);
        break;
      case 'REPLACE':
        quill.deleteText(operation.position, operation.length || 0);
        quill.insertText(operation.position, operation.content || '');
        break;
    }
  };

  const handleSave = async () => {
    if (!document) return;
    
    setSaving(true);
    try {
      await updateDocument(docId, { content });
      message.success('保存成功');
    } catch (error: any) {
      message.error('保存失败');
    } finally {
      setSaving(false);
    }
  };

  const handleAddComment = async () => {
    if (!newComment.trim()) {
      message.warning('请输入评论内容');
      return;
    }

    try {
      await createComment({
        documentId: docId,
        content: newComment,
      });
      setNewComment('');
      fetchComments();
      message.success('评论添加成功');
    } catch (error) {
      message.error('添加评论失败');
    }
  };

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '100px 0' }}>
        <Spin size="large" />
      </div>
    );
  }

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header style={{ background: '#fff', padding: '0 24px', borderBottom: '1px solid #f0f0f0' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Space>
            <Button
              type="text"
              icon={<ArrowLeftOutlined />}
              onClick={() => router.push('/documents')}
            >
              返回
            </Button>
            <Title level={4} style={{ margin: 0 }}>
              {document?.title}
            </Title>
            <Tag color={saving ? 'orange' : 'green'}>
              {saving ? '保存中...' : '已保存'}
            </Tag>
          </Space>

          <Space>
            <Button
              type="primary"
              icon={<SaveOutlined />}
              onClick={handleSave}
              loading={saving}
            >
              保存
            </Button>
            <Badge count={onlineUsers.length}>
              <Button icon={<UsergroupAddOutlined />}>
                在线用户
              </Button>
            </Badge>
            <Badge count={comments.length}>
              <Button
                icon={<CommentOutlined />}
                onClick={() => setCommentDrawerVisible(true)}
              >
                评论
              </Button>
            </Badge>
          </Space>
        </div>
      </Header>

      <Layout>
        <Content style={{ padding: '24px', background: '#fff' }}>
          <div ref={(el) => { if (el) quillRef.current = el.querySelector('.quill'); }}>
            <ReactQuill
              theme="snow"
              value={content}
              onChange={setContent}
              style={{ height: '600px' }}
              modules={{
                toolbar: [
                  [{ header: [1, 2, 3, 4, 5, 6, false] }],
                  ['bold', 'italic', 'underline', 'strike'],
                  [{ list: 'ordered' }, { list: 'bullet' }],
                  [{ color: [] }, { background: [] }],
                  ['link', 'image'],
                  ['clean'],
                ],
              }}
            />
          </div>
        </Content>

        <Sider width={80} style={{ background: '#fff', borderLeft: '1px solid #f0f0f0', padding: '16px' }}>
          <Space direction="vertical" size="middle">
            {onlineUsers.map((user) => (
              <Avatar
                key={user.userId}
                style={{ backgroundColor: user.color }}
                size="large"
              >
                {user.username.charAt(0).toUpperCase()}
              </Avatar>
            ))}
          </Space>
        </Sider>
      </Layout>

      {/* 评论抽屉 */}
      <Drawer
        title="评论"
        placement="right"
        width={400}
        onClose={() => setCommentDrawerVisible(false)}
        open={commentDrawerVisible}
      >
        <Space direction="vertical" style={{ width: '100%' }} size="large">
          <div>
            <TextArea
              rows={4}
              placeholder="输入评论..."
              value={newComment}
              onChange={(e) => setNewComment(e.target.value)}
            />
            <Button
              type="primary"
              style={{ marginTop: 8 }}
              onClick={handleAddComment}
            >
              添加评论
            </Button>
          </div>

          <List
            dataSource={comments}
            renderItem={(comment) => (
              <List.Item>
                <List.Item.Meta
                  avatar={<Avatar>{comment.username.charAt(0).toUpperCase()}</Avatar>}
                  title={comment.username}
                  description={
                    <>
                      <div>{comment.content}</div>
                      <Text type="secondary" style={{ fontSize: 12 }}>
                        {new Date(comment.createdAt).toLocaleString()}
                      </Text>
                    </>
                  }
                />
              </List.Item>
            )}
          />
        </Space>
      </Drawer>
    </Layout>
  );
}
