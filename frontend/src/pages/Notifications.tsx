import React, { useEffect, useState } from 'react';
import { List, Card, Tag, Button, Empty, message, Tabs, Space } from 'antd';
import {
  BellOutlined,
  CheckOutlined,
  FileTextOutlined,
  TeamOutlined,
  UserAddOutlined,
  CommentOutlined,
  CloseOutlined,
} from '@ant-design/icons';
import { notificationApi, collaboratorApi } from '../api';
import heartbeatService from '../utils/heartbeat';
import type { Notification, WorkspaceRequest } from '../types';
import dayjs from 'dayjs';
import './Notifications.scss';

const Notifications: React.FC = () => {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(false);
  const [invites, setInvites] = useState<WorkspaceRequest[]>([]);
  const [invitesLoading, setInvitesLoading] = useState(false);

  useEffect(() => {
    fetchNotifications();
    fetchInvites();
  }, []);

  const fetchNotifications = async () => {
    setLoading(true);
    try {
      const data = await notificationApi.getList();
      setNotifications(data.items);
    } catch (error) {
      console.error('Failed to fetch notifications:', error);
    } finally {
      setLoading(false);
    }
  };

  const fetchInvites = async () => {
    setInvitesLoading(true);
    try {
      const data = await collaboratorApi.getMyPendingInvites();
      setInvites(data);
    } catch (error) {
      console.error('Failed to fetch invites:', error);
    } finally {
      setInvitesLoading(false);
    }
  };

  const handleAcceptInvite = async (inviteId: number) => {
    try {
      await collaboratorApi.acceptInvite(inviteId);
      message.success('已接受邀请');
      setInvites(prev => prev.filter(i => i.id !== inviteId));
      // 刷新心跳，更新侧边栏 badge
      heartbeatService.refresh();
    } catch (error: any) {
      message.error(error.response?.data?.message || '操作失败');
    }
  };

  const handleRejectInvite = async (inviteId: number) => {
    try {
      await collaboratorApi.rejectInvite(inviteId);
      message.success('已拒绝邀请');
      setInvites(prev => prev.filter(i => i.id !== inviteId));
      // 刷新心跳，更新侧边栏 badge
      heartbeatService.refresh();
    } catch (error: any) {
      message.error(error.response?.data?.message || '操作失败');
    }
  };

  const handleMarkAsRead = async (id: number) => {
    try {
      await notificationApi.markAsRead(id);
      setNotifications(prev =>
        prev.map(n => n.id === id ? { ...n, isRead: true } : n)
      );
      window.dispatchEvent(new Event('notification-read'));
    } catch (error: any) {
      message.error('操作失败');
    }
  };

  const getNotificationIcon = (type: string) => {
    switch (type) {
      case 'DOCUMENT':
        return <FileTextOutlined />;
      case 'COLLABORATOR':
        return <TeamOutlined />;
      case 'FRIEND':
        return <UserAddOutlined />;
      case 'COMMENT':
        return <CommentOutlined />;
      default:
        return <BellOutlined />;
    }
  };

  const getNotificationColor = (type: string) => {
    switch (type) {
      case 'DOCUMENT':
        return 'blue';
      case 'COLLABORATOR':
        return 'green';
      case 'FRIEND':
        return 'purple';
      case 'COMMENT':
        return 'orange';
      default:
        return 'default';
    }
  };

  const notificationTab = (
    <List
      loading={loading}
      dataSource={notifications}
      locale={{ emptyText: <Empty description="暂无通知" /> }}
      renderItem={(notification) => (
        <List.Item
          className={`notification-item ${!notification.isRead ? 'unread' : ''}`}
          actions={[
            !notification.isRead && (
              <Button
                type="text"
                icon={<CheckOutlined />}
                onClick={() => handleMarkAsRead(notification.id)}
              >
                标记已读
              </Button>
            ),
          ]}
        >
          <List.Item.Meta
            avatar={
              <div className="notification-icon">
                {getNotificationIcon(notification.type)}
              </div>
            }
            title={
              <div className="notification-title">
                <span>{notification.title}</span>
                <Tag color={getNotificationColor(notification.type)}>
                  {notification.type}
                </Tag>
                {!notification.isRead && <span className="unread-dot" />}
              </div>
            }
            description={
              <>
                <div className="notification-content">{notification.content}</div>
                <div className="notification-time">
                  {dayjs(notification.createdAt).format('YYYY-MM-DD HH:mm')}
                </div>
              </>
            }
          />
        </List.Item>
      )}
    />
  );

  const invitesTab = (
    <List
      loading={invitesLoading}
      dataSource={invites}
      locale={{ emptyText: <Empty description="暂无待处理的协作邀请" /> }}
      renderItem={(invite) => (
        <List.Item
          className="notification-item"
          actions={[
            <Space>
              <Button
                type="primary"
                size="small"
                icon={<CheckOutlined />}
                onClick={() => handleAcceptInvite(invite.id)}
              >
                接受
              </Button>
              <Button
                danger
                size="small"
                icon={<CloseOutlined />}
                onClick={() => handleRejectInvite(invite.id)}
              >
                拒绝
              </Button>
            </Space>,
          ]}
        >
          <List.Item.Meta
            avatar={
              <div className="notification-icon">
                <TeamOutlined />
              </div>
            }
            title={
              <div className="notification-title">
                <span>协作邀请</span>
                <Tag color="green">INVITE</Tag>
              </div>
            }
            description={
              <>
                <div className="notification-content">
                  <strong>{invite.user?.username || '用户'}</strong> 邀请您协作编辑文档
                  「<strong>{invite.document?.title || '未知文档'}</strong>」
                </div>
                {invite.message && (
                  <div className="notification-content" style={{ color: '#666' }}>
                    留言：{invite.message}
                  </div>
                )}
                <div className="notification-time">
                  {dayjs(invite.createdAt).format('YYYY-MM-DD HH:mm')}
                </div>
              </>
            }
          />
        </List.Item>
      )}
    />
  );

  return (
    <div className="notifications-page">
      <Card title="消息中心">
        <Tabs
          defaultActiveKey="notifications"
          items={[
            {
              key: 'notifications',
              label: `系统通知 (${notifications.filter(n => !n.isRead).length})`,
              children: notificationTab,
            },
            {
              key: 'invites',
              label: `协作邀请 (${invites.length})`,
              children: invitesTab,
            },
          ]}
        />
      </Card>
    </div>
  );
};

export default Notifications;
