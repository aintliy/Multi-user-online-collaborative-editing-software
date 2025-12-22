'use client';

import React, { useEffect, useState } from 'react';
import { Badge, Dropdown, List, Button, Empty, Spin } from 'antd';
import { BellOutlined } from '@ant-design/icons';
import {
  getNotifications,
  markAsRead,
  markAllAsRead,
  getUnreadCount,
  NotificationVO,
} from '@/lib/api/notification';
import { websocketClient } from '@/lib/websocket';

export default function NotificationBell() {
  const [notifications, setNotifications] = useState<NotificationVO[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [dropdownOpen, setDropdownOpen] = useState(false);

  useEffect(() => {
    fetchUnreadCount();
    
    // 订阅WebSocket通知
    const token = localStorage.getItem('token');
    if (token && !websocketClient.isConnected()) {
      websocketClient.connect(token).then(() => {
        websocketClient.subscribeNotifications(handleNewNotification);
      });
    } else if (websocketClient.isConnected()) {
      websocketClient.subscribeNotifications(handleNewNotification);
    }
  }, []);

  const fetchUnreadCount = async () => {
    try {
      const count = await getUnreadCount();
      setUnreadCount(count);
    } catch (error) {
      console.error('Failed to fetch unread count:', error);
    }
  };

  const fetchNotifications = async () => {
    setLoading(true);
    try {
      const result = await getNotifications({ page: 1, size: 10 });
      setNotifications(result.records);
    } catch (error) {
      console.error('Failed to fetch notifications:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleNewNotification = (notification: any) => {
    // 新通知到达
    setUnreadCount((prev) => prev + 1);
    if (dropdownOpen) {
      fetchNotifications();
    }
  };

  const handleDropdownOpenChange = (open: boolean) => {
    setDropdownOpen(open);
    if (open) {
      fetchNotifications();
    }
  };

  const handleMarkAsRead = async (id: number) => {
    try {
      await markAsRead(id);
      setUnreadCount((prev) => Math.max(0, prev - 1));
      fetchNotifications();
    } catch (error) {
      console.error('Failed to mark as read:', error);
    }
  };

  const handleMarkAllAsRead = async () => {
    try {
      await markAllAsRead();
      setUnreadCount(0);
      fetchNotifications();
    } catch (error) {
      console.error('Failed to mark all as read:', error);
    }
  };

  const getNotificationTypeColor = (type: string) => {
    switch (type) {
      case 'COMMENT':
        return '#1890ff';
      case 'TASK':
        return '#52c41a';
      case 'PERMISSION':
        return '#faad14';
      default:
        return '#666';
    }
  };

  const dropdownRender = () => (
    <div
      style={{
        width: 350,
        maxHeight: 400,
        background: '#fff',
        borderRadius: 8,
        boxShadow: '0 2px 8px rgba(0,0,0,0.15)',
      }}
    >
      <div
        style={{
          padding: '12px 16px',
          borderBottom: '1px solid #f0f0f0',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
        }}
      >
        <span style={{ fontWeight: 'bold' }}>通知</span>
        {unreadCount > 0 && (
          <Button type="link" size="small" onClick={handleMarkAllAsRead}>
            全部已读
          </Button>
        )}
      </div>

      {loading ? (
        <div style={{ textAlign: 'center', padding: 50 }}>
          <Spin />
        </div>
      ) : notifications.length === 0 ? (
        <Empty
          image={Empty.PRESENTED_IMAGE_SIMPLE}
          description="暂无通知"
          style={{ padding: 50 }}
        />
      ) : (
        <List
          style={{ maxHeight: 300, overflow: 'auto' }}
          dataSource={notifications}
          renderItem={(item) => (
            <List.Item
              style={{
                padding: '12px 16px',
                cursor: 'pointer',
                background: item.isRead ? '#fff' : '#f0f5ff',
              }}
              onClick={() => !item.isRead && handleMarkAsRead(item.id)}
            >
              <List.Item.Meta
                title={
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <div
                      style={{
                        width: 6,
                        height: 6,
                        borderRadius: '50%',
                        background: getNotificationTypeColor(item.type),
                      }}
                    />
                    <span>{item.title}</span>
                  </div>
                }
                description={
                  <>
                    <div style={{ marginTop: 4 }}>{item.content}</div>
                    <div style={{ marginTop: 4, fontSize: 12, color: '#999' }}>
                      {new Date(item.createdAt).toLocaleString()}
                    </div>
                  </>
                }
              />
            </List.Item>
          )}
        />
      )}
    </div>
  );

  return (
    <Dropdown
      popupRender={dropdownRender}
      trigger={['click']}
      open={dropdownOpen}
      onOpenChange={handleDropdownOpenChange}
    >
      <Badge count={unreadCount} offset={[-5, 5]}>
        <Button
          type="text"
          icon={<BellOutlined style={{ fontSize: 18 }} />}
          style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}
        />
      </Badge>
    </Dropdown>
  );
}
