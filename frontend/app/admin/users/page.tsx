'use client';

import React, { useEffect, useState } from 'react';
import {
  Card,
  Table,
  Space,
  Button,
  Modal,
  Form,
  Select,
  message,
  Tag,
  Typography,
  Input,
} from 'antd';
import { EditOutlined, SearchOutlined } from '@ant-design/icons';
import {
  getAllUsers,
  getAllRoles,
  getUserRoles,
  updateUserRoles,
  UserVO,
  RoleVO,
  PageResult,
} from '@/lib/api/admin';

const { Title } = Typography;
const { Option } = Select;

export default function UsersPage() {
  const [users, setUsers] = useState<UserVO[]>([]);
  const [roles, setRoles] = useState<RoleVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize] = useState(10);
  const [keyword, setKeyword] = useState('');
  const [modalVisible, setModalVisible] = useState(false);
  const [editingUser, setEditingUser] = useState<UserVO | null>(null);
  const [selectedRoles, setSelectedRoles] = useState<number[]>([]);
  const [form] = Form.useForm();

  useEffect(() => {
    fetchUsers();
    fetchRoles();
  }, [currentPage, keyword]);

  const fetchUsers = async () => {
    setLoading(true);
    try {
      const result: PageResult<UserVO> = await getAllUsers(currentPage, pageSize, keyword);
      setUsers(result.records);
      setTotal(result.total);
    } catch (error: any) {
      message.error('获取用户列表失败');
    } finally {
      setLoading(false);
    }
  };

  const fetchRoles = async () => {
    try {
      const data = await getAllRoles();
      setRoles(data);
    } catch (error) {
      console.error('Failed to fetch roles:', error);
    }
  };

  const handleEditRoles = async (user: UserVO) => {
    setEditingUser(user);
    try {
      const userRoles = await getUserRoles(user.id);
      setSelectedRoles(userRoles.map((r) => r.id));
      form.setFieldsValue({
        roleIds: userRoles.map((r) => r.id),
      });
      setModalVisible(true);
    } catch (error) {
      message.error('获取用户角色失败');
    }
  };

  const handleSubmit = async (values: any) => {
    if (!editingUser) return;

    try {
      await updateUserRoles({
        userId: editingUser.id,
        roleIds: values.roleIds,
      });
      message.success('角色更新成功');
      setModalVisible(false);
      form.resetFields();
      fetchUsers();
    } catch (error: any) {
      message.error(error.message || '更新失败');
    }
  };

  const getStatusColor = (status: string) => {
    return status === 'ACTIVE' ? 'success' : 'default';
  };

  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
    },
    {
      title: '用户名',
      dataIndex: 'username',
      key: 'username',
      width: 150,
    },
    {
      title: '邮箱',
      dataIndex: 'email',
      key: 'email',
    },
    {
      title: '角色',
      dataIndex: 'roles',
      key: 'roles',
      render: (roles: string[]) => (
        <>
          {roles.map((role) => (
            <Tag key={role} color="blue">
              {role}
            </Tag>
          ))}
        </>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: string) => (
        <Tag color={getStatusColor(status)}>
          {status === 'ACTIVE' ? '激活' : '禁用'}
        </Tag>
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
      render: (date: string) => new Date(date).toLocaleString(),
    },
    {
      title: '操作',
      key: 'action',
      width: 120,
      render: (_: any, record: UserVO) => (
        <Space>
          <Button
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEditRoles(record)}
          >
            编辑角色
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <Card>
        <Space direction="vertical" size="large" style={{ width: '100%' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Title level={3} style={{ margin: 0 }}>
              用户管理
            </Title>
            <Space>
              <Input
                placeholder="搜索用户名或邮箱"
                prefix={<SearchOutlined />}
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                style={{ width: 250 }}
              />
            </Space>
          </div>

          <Table
            columns={columns}
            dataSource={users}
            rowKey="id"
            loading={loading}
            pagination={{
              current: currentPage,
              pageSize: pageSize,
              total: total,
              onChange: (page) => setCurrentPage(page),
              showTotal: (total) => `共 ${total} 条`,
            }}
          />
        </Space>
      </Card>

      {/* 编辑角色对话框 */}
      <Modal
        title="编辑用户角色"
        open={modalVisible}
        onCancel={() => {
          setModalVisible(false);
          form.resetFields();
        }}
        onOk={() => form.submit()}
        width={500}
      >
        <Form form={form} layout="vertical" onFinish={handleSubmit}>
          <Form.Item label="用户">
            <Input value={editingUser?.username} disabled />
          </Form.Item>

          <Form.Item
            label="角色"
            name="roleIds"
            rules={[{ required: true, message: '请选择至少一个角色' }]}
          >
            <Select mode="multiple" placeholder="选择角色">
              {roles.map((role) => (
                <Option key={role.id} value={role.id}>
                  {role.name} - {role.description}
                </Option>
              ))}
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
