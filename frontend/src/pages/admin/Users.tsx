import React, { useEffect, useState } from 'react';
import {
  Table,
  Card,
  Input,
  Button,
  Tag,
  Space,
  Avatar,
  Modal,
  message,
  Select,
  Badge,
  Tooltip,
} from 'antd';
import { UserOutlined, StopOutlined, CheckCircleOutlined, DeleteOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { adminApi } from '../../api';
import { getAvatarUrl } from '../../utils/request';
import type { User } from '../../types';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import 'dayjs/locale/zh-cn';

dayjs.extend(relativeTime);
dayjs.locale('zh-cn');

const { Search } = Input;

const Users: React.FC = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [keyword, setKeyword] = useState('');
  const [statusFilter, setStatusFilter] = useState<string | undefined>();

  useEffect(() => {
    fetchUsers();
  }, [page, pageSize, keyword, statusFilter]);

  const fetchUsers = async () => {
    setLoading(true);
    try {
      const data = await adminApi.getUsers({
        keyword: keyword || undefined,
        status: statusFilter,
        page,
        pageSize,
      });
      setUsers(data.items);
      setTotal(data.total);
    } catch (error) {
      console.error('Failed to fetch users:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleBanUser = async (userId: number) => {
    Modal.confirm({
      title: '确认禁用',
      content: '确定要禁用该用户吗？',
      onOk: async () => {
        try {
          await adminApi.banUser(userId);
          message.success('用户已禁用');
          fetchUsers();
        } catch (error: any) {
          message.error(error.response?.data?.message || '操作失败');
        }
      },
    });
  };

  const handleUnbanUser = async (userId: number) => {
    try {
      await adminApi.unbanUser(userId);
      message.success('用户已解禁');
      fetchUsers();
    } catch (error: any) {
      message.error(error.response?.data?.message || '操作失败');
    }
  };

  const handleDeleteUser = async (userId: number) => {
    Modal.confirm({
      title: '确认删除',
      content: '确定要删除该用户吗？此操作不可恢复。',
      okType: 'danger',
      onOk: async () => {
        try {
          await adminApi.deleteUser(userId);
          message.success('用户已删除');
          fetchUsers();
        } catch (error: any) {
          message.error(error.response?.data?.message || '删除失败');
        }
      },
    });
  };

  // 判断用户是否在线（5分钟内活跃）
  const isOnline = (lastLoginAt?: string) => {
    if (!lastLoginAt) return false;
    return dayjs().diff(dayjs(lastLoginAt), 'minute') < 5;
  };

  // 获取活跃状态
  const getActivityStatus = (lastLoginAt?: string) => {
    if (!lastLoginAt) return { text: '从未登录', color: 'default' };
    const diffMinutes = dayjs().diff(dayjs(lastLoginAt), 'minute');
    if (diffMinutes < 5) return { text: '在线', color: 'green' };
    if (diffMinutes < 60) return { text: dayjs(lastLoginAt).fromNow(), color: 'blue' };
    if (diffMinutes < 1440) return { text: dayjs(lastLoginAt).fromNow(), color: 'orange' };
    return { text: dayjs(lastLoginAt).fromNow(), color: 'default' };
  };

  const columns: ColumnsType<User> = [
    {
      title: '用户',
      key: 'user',
      render: (_, record) => {
        const online = isOnline(record.lastLoginAt);
        return (
          <Space>
            <Badge dot status={online ? 'success' : 'default'} offset={[-4, 32]}>
              <Avatar src={getAvatarUrl(record.avatarUrl)} icon={<UserOutlined />} />
            </Badge>
            <span>{record.username}</span>
          </Space>
        );
      },
    },
    {
      title: '邮箱',
      dataIndex: 'email',
    },
    {
      title: '角色',
      dataIndex: 'role',
      render: (role: string) => (
        <Tag color={role === 'ADMIN' ? 'gold' : 'blue'}>
          {role === 'ADMIN' ? '管理员' : '普通用户'}
        </Tag>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      render: (status: string) => (
        <Tag color={status === 'ACTIVE' ? 'green' : 'red'}>
          {status === 'ACTIVE' ? '正常' : '已禁用'}
        </Tag>
      ),
    },
    {
      title: '活跃状态',
      key: 'activity',
      render: (_, record) => {
        const activity = getActivityStatus(record.lastLoginAt);
        return (
          <Tooltip title={record.lastLoginAt ? `最后登录: ${dayjs(record.lastLoginAt).format('YYYY-MM-DD HH:mm:ss')}` : '从未登录'}>
            <Tag color={activity.color}>{activity.text}</Tag>
          </Tooltip>
        );
      },
    },
    {
      title: '注册时间',
      dataIndex: 'createdAt',
      render: (date: string) => dayjs(date).format('YYYY-MM-DD HH:mm'),
    },
    {
      title: '操作',
      key: 'actions',
      render: (_, record) => (
        <Space>
          {record.status === 'ACTIVE' ? (
            <Button
              size="small"
              icon={<StopOutlined />}
              onClick={() => handleBanUser(record.id)}
            >
              禁用
            </Button>
          ) : (
            <Button
              size="small"
              icon={<CheckCircleOutlined />}
              onClick={() => handleUnbanUser(record.id)}
            >
              解禁
            </Button>
          )}
          <Button
            size="small"
            danger
            icon={<DeleteOutlined />}
            onClick={() => handleDeleteUser(record.id)}
          >
            删除
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <Card title="用户管理">
      <Space style={{ marginBottom: 16 }}>
        <Search
          placeholder="搜索用户名或邮箱"
          allowClear
          onSearch={setKeyword}
          style={{ width: 240 }}
        />
        <Select
          placeholder="筛选状态"
          allowClear
          style={{ width: 120 }}
          onChange={setStatusFilter}
          options={[
            { value: 'ACTIVE', label: '正常' },
            { value: 'BANNED', label: '已禁用' },
          ]}
        />
      </Space>

      <Table
        columns={columns}
        dataSource={users}
        rowKey="id"
        loading={loading}
        pagination={{
          current: page,
          pageSize,
          total,
          showSizeChanger: true,
          showTotal: (total) => `共 ${total} 条`,
          onChange: (page, pageSize) => {
            setPage(page);
            setPageSize(pageSize);
          },
        }}
      />
    </Card>
  );
};

export default Users;
