import React, { useEffect, useState } from 'react';
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
} from 'antd';
import {
  UserOutlined,
  UserAddOutlined,
  CheckOutlined,
  CloseOutlined,
  DeleteOutlined,
} from '@ant-design/icons';
import { friendApi, userApi } from '../api';
import type { Friend, User } from '../types';
import dayjs from 'dayjs';
import './Friends.scss';

const { Search } = Input;

const Friends: React.FC = () => {
  const [friends, setFriends] = useState<Friend[]>([]);
  const [requests, setRequests] = useState<Friend[]>([]);
  const [searchResults, setSearchResults] = useState<User[]>([]);
  const [loading, setLoading] = useState(false);
  const [addModalOpen, setAddModalOpen] = useState(false);
  const [searchKeyword, setSearchKeyword] = useState('');

  useEffect(() => {
    fetchFriends();
    fetchRequests();
  }, []);

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

  const tabItems = [
    {
      key: 'friends',
      label: `我的好友 (${friends.length})`,
      children: (
        <List
          loading={loading}
          dataSource={friends}
          locale={{ emptyText: <Empty description="暂无好友" /> }}
          renderItem={(friend) => (
            <List.Item
              actions={[
                <Button
                  type="text"
                  danger
                  icon={<DeleteOutlined />}
                  onClick={() => handleDeleteFriend(friend.friend!.id)}
                >
                  删除
                </Button>,
              ]}
            >
              <List.Item.Meta
                avatar={<Avatar src={friend.friend?.avatarUrl} icon={<UserOutlined />} />}
                title={friend.friend?.username}
                description={friend.friend?.email}
              />
            </List.Item>
          )}
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
                avatar={<Avatar src={request.user?.avatarUrl} icon={<UserOutlined />} />}
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
                  type="primary"
                  size="small"
                  onClick={() => handleSendRequest(user.id)}
                >
                  添加
                </Button>,
              ]}
            >
              <List.Item.Meta
                avatar={<Avatar src={user.avatarUrl} icon={<UserOutlined />} />}
                title={user.username}
                description={user.email}
              />
            </List.Item>
          )}
        />
      </Modal>
    </div>
  );
};

export default Friends;
