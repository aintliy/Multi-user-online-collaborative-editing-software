import React, { useEffect, useState } from 'react';
import { List, Card, Tag, Button, Empty, message } from 'antd';
import {
  BellOutlined,
  CheckOutlined,
  FileTextOutlined,
  TeamOutlined,
  UserAddOutlined,
  CommentOutlined,
} from '@ant-design/icons';
import { notificationApi } from '../api';
import type { Notification } from '../types';
import dayjs from 'dayjs';
import './Notifications.scss';

const Notifications: React.FC = () => {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchNotifications();
  }, []);

  const fetchNotifications = async () => {
    setLoading(true);
    try {
      const data = await notificationApi.getList();
      setNotifications(data.content);
    } catch (error) {
      console.error('Failed to fetch notifications:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleMarkAsRead = async (id: number) => {
    try {
      await notificationApi.markAsRead(id);
      setNotifications(prev =>
        prev.map(n => n.id === id ? { ...n, isRead: true } : n)
      );
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

  return (
    <div className="notifications-page">
      <Card title="通知">
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
      </Card>
    </div>
  );
};

export default Notifications;
