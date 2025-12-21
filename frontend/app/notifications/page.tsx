'use client';

import React, { useState, useEffect } from 'react';
import { Card, List, Tag, Button, Space, Select, Spin, message, Empty } from 'antd';
import { BellOutlined, CheckOutlined, DeleteOutlined } from '@ant-design/icons';
import { getNotifications, markAsRead, markAllAsRead, deleteNotification } from '@/lib/api/notification';
import type { NotificationVO } from '@/lib/api/notification';

const { Option } = Select;

export default function NotificationsPage() {
  const [notifications, setNotifications] = useState<NotificationVO[]>([]);
  const [loading, setLoading] = useState(true);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 20,
    total: 0,
  });
  const [filters, setFilters] = useState<{
    isRead?: boolean;
    type?: string;
  }>({});

  useEffect(() => {
    loadNotifications();
  }, [pagination.current, filters]);

  const loadNotifications = async () => {
    setLoading(true);
    try {
      const response = await getNotifications({
        page: pagination.current,
        size: pagination.pageSize,
        isRead: filters.isRead,
        type: filters.type,
      });
      setNotifications(response.records);
      setPagination(prev => ({
        ...prev,
        total: response.total,
      }));
    } catch (error: any) {
      message.error(error.message || '加载通知失败');
    } finally {
      setLoading(false);
    }
  };

  const handleMarkAsRead = async (id: number) => {
    try {
      await markAsRead(id);
      message.success('已标记为已读');
      loadNotifications();
    } catch (error: any) {
      message.error(error.message || '操作失败');
    }
  };

  const handleMarkAllAsRead = async () => {
    try {
      await markAllAsRead();
      message.success('已全部标记为已读');
      loadNotifications();
    } catch (error: any) {
      message.error(error.message || '操作失败');
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await deleteNotification(id);
      message.success('已删除');
      loadNotifications();
    } catch (error: any) {
      message.error(error.message || '删除失败');
    }
  };

  const getTypeTag = (type: string) => {
    const typeMap: Record<string, { text: string; color: string }> = {
      COMMENT: { text: '评论', color: 'blue' },
      TASK: { text: '任务', color: 'green' },
      PERMISSION: { text: '权限', color: 'orange' },
      MENTION: { text: '提及', color: 'purple' },
      SYSTEM: { text: '系统', color: 'default' },
    };
    const info = typeMap[type] || { text: type, color: 'default' };
    return <Tag color={info.color}>{info.text}</Tag>;
  };

  const handleLoadMore = () => {
    setPagination(prev => ({
      ...prev,
      current: prev.current + 1,
    }));
  };

  return (
    <div className="max-w-4xl mx-auto p-6">
      <Card
        title={
          <Space>
            <BellOutlined />
            <span>通知中心</span>
          </Space>
        }
        extra={
          <Space>
            <Select
              placeholder="状态筛选"
              style={{ width: 120 }}
              allowClear
              onChange={(value) => {
                setFilters(prev => ({ ...prev, isRead: value }));
                setPagination(prev => ({ ...prev, current: 1 }));
              }}
            >
              <Option value={false}>未读</Option>
              <Option value={true}>已读</Option>
            </Select>

            <Select
              placeholder="类型筛选"
              style={{ width: 120 }}
              allowClear
              onChange={(value) => {
                setFilters(prev => ({ ...prev, type: value }));
                setPagination(prev => ({ ...prev, current: 1 }));
              }}
            >
              <Option value="COMMENT">评论</Option>
              <Option value="TASK">任务</Option>
              <Option value="PERMISSION">权限</Option>
              <Option value="MENTION">提及</Option>
            </Select>

            <Button onClick={handleMarkAllAsRead}>
              全部已读
            </Button>
          </Space>
        }
      >
        {loading && notifications.length === 0 ? (
          <div className="text-center py-12">
            <Spin size="large" />
          </div>
        ) : notifications.length === 0 ? (
          <Empty description="暂无通知" />
        ) : (
          <>
            <List
              itemLayout="horizontal"
              dataSource={notifications}
              renderItem={(item) => (
                <List.Item
                  className={`${!item.isRead ? 'bg-blue-50' : ''} hover:bg-gray-50 transition-colors`}
                  actions={[
                    !item.isRead && (
                      <Button
                        type="link"
                        icon={<CheckOutlined />}
                        onClick={() => handleMarkAsRead(item.id)}
                      >
                        标记已读
                      </Button>
                    ),
                    <Button
                      type="link"
                      danger
                      icon={<DeleteOutlined />}
                      onClick={() => handleDelete(item.id)}
                    >
                      删除
                    </Button>,
                  ].filter(Boolean)}
                >
                  <List.Item.Meta
                    avatar={
                      <div className="pt-1">
                        {getTypeTag(item.type)}
                      </div>
                    }
                    title={
                      <div className="flex items-center gap-2">
                        <span className={!item.isRead ? 'font-semibold' : ''}>
                          {item.title}
                        </span>
                        {!item.isRead && (
                          <div className="w-2 h-2 bg-blue-500 rounded-full" />
                        )}
                      </div>
                    }
                    description={
                      <div>
                        <div className="mb-2">{item.content}</div>
                        <div className="text-gray-400 text-xs">
                          {new Date(item.createdAt).toLocaleString('zh-CN')}
                        </div>
                      </div>
                    }
                  />
                </List.Item>
              )}
            />

            {notifications.length < pagination.total && (
              <div className="text-center mt-4">
                <Button onClick={handleLoadMore} loading={loading}>
                  加载更多
                </Button>
              </div>
            )}
          </>
        )}
      </Card>
    </div>
  );
}
