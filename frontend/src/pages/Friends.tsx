import React, { useEffect, useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Card,
  Tabs,
  List,
  Avatar,
  Button,
  Input,
  Empty,
  Modal,
  message,
  Tag,
  Drawer,
  Space,
} from 'antd';
import {
  UserOutlined,
  UserAddOutlined,
  CheckOutlined,
  CloseOutlined,
  DeleteOutlined,
  MessageOutlined,
  SendOutlined,
  FolderOpenOutlined,
} from '@ant-design/icons';
import { friendApi, userApi } from '../api';
import type { Friend, FriendMessage, User } from '../types';
import { useAuthStore } from '../store/useAuthStore';
import { getAvatarUrl } from '../utils/request';
import dayjs from 'dayjs';
import './Friends.scss';

const { Search, TextArea } = Input;

const Friends: React.FC = () => {
  const navigate = useNavigate();
  const currentUser = useAuthStore((state) => state.user);
  const [friends, setFriends] = useState<Friend[]>([]);
  const [requests, setRequests] = useState<Friend[]>([]);
  const [searchResults, setSearchResults] = useState<User[]>([]);
  const [loading, setLoading] = useState(false);
  const [addModalOpen, setAddModalOpen] = useState(false);
  const [searchKeyword, setSearchKeyword] = useState('');

  // 聊天相关状态
  const [chatDrawerOpen, setChatDrawerOpen] = useState(false);
  const [chatFriend, setChatFriend] = useState<User | null>(null);
  const [chatMessages, setChatMessages] = useState<FriendMessage[]>([]);
  const [newMessage, setNewMessage] = useState('');
  const [chatLoading, setChatLoading] = useState(false);
  const chatContainerRef = useRef<HTMLDivElement>(null);

  // 辅助函数：从好友关系中获取"对方"的用户信息
  const getOtherUser = (friend: Friend): User | undefined => {
    if (!currentUser) return undefined;
    // 如果当前用户是发起者(user)，返回friend；否则返回user
    return friend.user?.id === currentUser.id ? friend.friend : friend.user;
  };

  useEffect(() => {
    fetchFriends();
    fetchRequests();

    // 监听实时消息通知
    const handleFriendMessage = (event: CustomEvent) => {
      const data = event.detail;
      // 如果当前正在和发送者聊天，直接添加消息
      if (chatFriend && data.senderId === chatFriend.id) {
        setChatMessages(prev => [...prev, data.message]);
      }
    };

    // 监听从通知打开聊天的事件
    const handleOpenFriendChat = (event: CustomEvent) => {
      const { friendId, friendName, friendAvatar } = event.detail;
      const friendUser: User = {
        id: friendId,
        publicId: '',
        username: friendName,
        email: '',
        avatarUrl: friendAvatar,
        role: 'USER',
        status: 'active',
      };
      handleOpenChat(friendUser);
    };

    window.addEventListener('friend-message-received', handleFriendMessage as EventListener);
    window.addEventListener('open-friend-chat', handleOpenFriendChat as EventListener);
    return () => {
      window.removeEventListener('friend-message-received', handleFriendMessage as EventListener);
      window.removeEventListener('open-friend-chat', handleOpenFriendChat as EventListener);
    };
  }, [chatFriend]);

  // 聊天消息滚动到底部
  useEffect(() => {
    if (chatContainerRef.current) {
      chatContainerRef.current.scrollTop = chatContainerRef.current.scrollHeight;
    }
  }, [chatMessages]);

  const fetchFriends = async () => {
    setLoading(true);
    try {
      const data = await friendApi.getList();
      setFriends(data);
    } catch (error) {
      console.error('Failed to fetch friends:', error);
    } finally {
      setLoading(false);
    }
  };

  const fetchRequests = async () => {
    try {
      const data = await friendApi.getRequests();
      setRequests(data);
    } catch (error) {
      console.error('Failed to fetch requests:', error);
    }
  };

  const handleSearchUsers = async (keyword: string) => {
    if (!keyword.trim()) {
      setSearchResults([]);
      return;
    }
    try {
      const users = await userApi.searchUsers(keyword);
      setSearchResults(users);
    } catch (error) {
      console.error('Failed to search users:', error);
    }
  };

  const handleSendRequest = async (userId: number) => {
    try {
      await friendApi.sendRequest(userId);
      message.success('好友请求已发送');
      setAddModalOpen(false);
      setSearchKeyword('');
      setSearchResults([]);
    } catch (error: any) {
      message.error(error.response?.data?.message || '发送失败');
    }
  };

  const handleAcceptRequest = async (requestId: number) => {
    try {
      await friendApi.acceptRequest(requestId);
      message.success('已接受好友请求');
      fetchFriends();
      fetchRequests();
    } catch (error: any) {
      message.error(error.response?.data?.message || '操作失败');
    }
  };

  const handleRejectRequest = async (requestId: number) => {
    try {
      await friendApi.rejectRequest(requestId);
      message.success('已拒绝好友请求');
      fetchRequests();
    } catch (error: any) {
      message.error(error.response?.data?.message || '操作失败');
    }
  };

  const handleDeleteFriend = async (friendUserId: number) => {
    Modal.confirm({
      title: '确认删除',
      content: '确定要删除该好友吗？',
      onOk: async () => {
        try {
          await friendApi.delete(friendUserId);
          message.success('好友已删除');
          fetchFriends();
        } catch (error: any) {
          message.error(error.response?.data?.message || '删除失败');
        }
      },
    });
  };

  // 查看用户公开仓库
  const handleViewUserRepos = (publicId: string) => {
    navigate(`/user/${publicId}`);
  };

  // 打开聊天
  const handleOpenChat = async (friend: User) => {
    setChatFriend(friend);
    setChatDrawerOpen(true);
    setChatLoading(true);
    
    // 通知 MainLayout 清除该好友的未读消息徽标
    window.dispatchEvent(new CustomEvent('friend-messages-viewed', { detail: { friendId: friend.id } }));
    
    try {
      const messages = await friendApi.getMessages(friend.id);
      setChatMessages(messages);
      await friendApi.markAsRead(friend.id);
    } catch (error) {
      console.error('Failed to fetch messages:', error);
    } finally {
      setChatLoading(false);
    }
  };

  // 发送消息
  const handleSendMessage = async () => {
    if (!newMessage.trim() || !chatFriend) return;
    try {
      const msg = await friendApi.sendMessage(chatFriend.id, newMessage.trim());
      setChatMessages(prev => [...prev, msg]);
      setNewMessage('');
    } catch (error: any) {
      message.error(error.response?.data?.message || '发送失败');
    }
  };

  // 渲染聊天消息
  const renderChatMessage = (msg: FriendMessage) => {
    const isOwn = msg.sender?.id === currentUser?.id;
    return (
      <div key={msg.id} className={`chat-message ${isOwn ? 'own' : ''}`}>
        {!isOwn && <Avatar size="small" src={getAvatarUrl(msg.sender?.avatarUrl)} icon={<UserOutlined />} />}
        <div className="message-content">
          <div className="message-text">{msg.content}</div>
          <div className="message-time">{dayjs(msg.createdAt).format('HH:mm')}</div>
        </div>
        {isOwn && <Avatar size="small" src={getAvatarUrl(currentUser?.avatarUrl)} icon={<UserOutlined />} />}
      </div>
    );
  };

  const tabItems = [
    {
      key: 'friends',
      label: `我的好友 (${friends.length})`,
      children: (
        <List
          loading={loading}
          dataSource={friends}
          locale={{ emptyText: <Empty description="暂无好友" /> }}
          renderItem={(friend) => {
            const otherUser = getOtherUser(friend);
            return (
              <List.Item
                actions={[
                  <Button
                    type="text"
                    icon={<MessageOutlined />}
                    onClick={() => otherUser && handleOpenChat(otherUser)}
                  >
                    聊天
                  </Button>,
                  <Button
                    type="text"
                    icon={<FolderOpenOutlined />}
                    onClick={() => otherUser?.publicId && handleViewUserRepos(otherUser.publicId)}
                  >
                    仓库
                  </Button>,
                  <Button
                    type="text"
                    danger
                    icon={<DeleteOutlined />}
                    onClick={() => handleDeleteFriend(otherUser?.id!)}
                  >
                    删除
                  </Button>,
                ]}
              >
                <List.Item.Meta
                  avatar={
                    <Avatar 
                      src={getAvatarUrl(otherUser?.avatarUrl)} 
                      icon={<UserOutlined />}
                      style={{ cursor: 'pointer' }}
                      onClick={() => otherUser?.publicId && handleViewUserRepos(otherUser.publicId)}
                    />
                  }
                  title={
                    <span 
                      style={{ cursor: 'pointer' }}
                      onClick={() => otherUser?.publicId && handleViewUserRepos(otherUser.publicId)}
                    >
                      {otherUser?.username}
                    </span>
                  }
                  description={otherUser?.email}
                />
              </List.Item>
            );
          }}
        />
      ),
    },
    {
      key: 'requests',
      label: (
        <span>
          好友请求
          {requests.length > 0 && <Tag color="red" style={{ marginLeft: 8 }}>{requests.length}</Tag>}
        </span>
      ),
      children: (
        <List
          dataSource={requests}
          locale={{ emptyText: <Empty description="暂无好友请求" /> }}
          renderItem={(request) => (
            <List.Item
              actions={[
                <Button
                  type="primary"
                  icon={<CheckOutlined />}
                  onClick={() => handleAcceptRequest(request.id)}
                >
                  接受
                </Button>,
                <Button
                  icon={<CloseOutlined />}
                  onClick={() => handleRejectRequest(request.id)}
                >
                  拒绝
                </Button>,
              ]}
            >
              <List.Item.Meta
                avatar={<Avatar src={getAvatarUrl(request.user?.avatarUrl)} icon={<UserOutlined />} />}
                title={request.user?.username}
                description={
                  <>
                    <div>{request.user?.email}</div>
                    <div className="request-time">
                      {dayjs(request.createdAt).format('YYYY-MM-DD HH:mm')}
                    </div>
                  </>
                }
              />
            </List.Item>
          )}
        />
      ),
    },
  ];

  return (
    <div className="friends-page">
      <Card
        title="好友"
        extra={
          <Button
            type="primary"
            icon={<UserAddOutlined />}
            onClick={() => setAddModalOpen(true)}
          >
            添加好友
          </Button>
        }
      >
        <Tabs items={tabItems} />
      </Card>

      <Modal
        title="添加好友"
        open={addModalOpen}
        onCancel={() => {
          setAddModalOpen(false);
          setSearchKeyword('');
          setSearchResults([]);
        }}
        footer={null}
      >
        <Search
          placeholder="搜索用户名或邮箱"
          value={searchKeyword}
          onChange={(e) => setSearchKeyword(e.target.value)}
          onSearch={handleSearchUsers}
          enterButton
          style={{ marginBottom: 16 }}
        />
        
        <List
          dataSource={searchResults}
          locale={{ emptyText: searchKeyword ? '未找到用户' : '请输入搜索关键词' }}
          renderItem={(user) => (
            <List.Item
              actions={[
                <Button
                  type="text"
                  icon={<FolderOpenOutlined />}
                  onClick={() => user.publicId && handleViewUserRepos(user.publicId)}
                >
                  仓库
                </Button>,
                <Button
                  type="primary"
                  size="small"
                  onClick={() => handleSendRequest(user.id)}
                >
                  添加
                </Button>,
              ]}
            >
              <List.Item.Meta
                avatar={
                  <Avatar 
                    src={getAvatarUrl(user.avatarUrl)} 
                    icon={<UserOutlined />}
                    style={{ cursor: 'pointer' }}
                    onClick={() => user.publicId && handleViewUserRepos(user.publicId)}
                  />
                }
                title={
                  <span 
                    style={{ cursor: 'pointer' }}
                    onClick={() => user.publicId && handleViewUserRepos(user.publicId)}
                  >
                    {user.username}
                  </span>
                }
                description={user.email}
              />
            </List.Item>
          )}
        />
      </Modal>

      {/* 聊天抽屉 */}
      <Drawer
        title={
          <Space>
            <Avatar src={getAvatarUrl(chatFriend?.avatarUrl)} icon={<UserOutlined />} />
            <span>{chatFriend?.username}</span>
          </Space>
        }
        open={chatDrawerOpen}
        onClose={() => {
          setChatDrawerOpen(false);
          setChatFriend(null);
          setChatMessages([]);
        }}
        size={400}
        className="chat-drawer"
      >
        <div className="chat-container">
          <div className="chat-messages" ref={chatContainerRef}>
            {chatLoading ? (
              <div className="chat-loading">加载中...</div>
            ) : chatMessages.length === 0 ? (
              <Empty description="暂无消息" />
            ) : (
              chatMessages.map(renderChatMessage)
            )}
          </div>
          <div className="chat-input">
            <TextArea
              value={newMessage}
              onChange={(e) => setNewMessage(e.target.value)}
              placeholder="输入消息..."
              autoSize={{ minRows: 1, maxRows: 3 }}
              onPressEnter={(e) => {
                if (!e.shiftKey) {
                  e.preventDefault();
                  handleSendMessage();
                }
              }}
            />
            <Button 
              type="primary" 
              icon={<SendOutlined />}
              onClick={handleSendMessage}
            />
          </div>
        </div>
      </Drawer>
    </div>
  );
};

export default Friends;
