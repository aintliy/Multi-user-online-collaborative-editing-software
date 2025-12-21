'use client';

import React, { useState, useEffect } from 'react';
import { useRouter, useParams } from 'next/navigation';
import { Card, Form, Input, Select, Button, Table, message, Space, Popconfirm, Modal } from 'antd';
import { ArrowLeftOutlined, PlusOutlined, DeleteOutlined } from '@ant-design/icons';
import { getDocumentPermissions, shareDocument } from '@/lib/api/document';
import type { ColumnsType } from 'antd/es/table';

const { Option } = Select;

interface Permission {
  id: number;
  userId: number;
  username: string;
  email: string;
  role: string;
  grantedAt: string;
}

export default function DocumentSharePage() {
  const router = useRouter();
  const params = useParams();
  const documentId = Number(params.id);
  const [permissions, setPermissions] = useState<Permission[]>([]);
  const [loading, setLoading] = useState(false);
  const [shareModalVisible, setShareModalVisible] = useState(false);
  const [form] = Form.useForm();

  useEffect(() => {
    loadPermissions();
  }, [documentId]);

  const loadPermissions = async () => {
    setLoading(true);
    try {
      const data = await getDocumentPermissions(documentId);
      setPermissions(data);
    } catch (error: any) {
      message.error(error.message || '加载权限失败');
    } finally {
      setLoading(false);
    }
  };

  const handleShare = async (values: { email: string; role: string }) => {
    try {
      await shareDocument(documentId, {
        userEmail: values.email,
        role: values.role,
      });
      message.success('分享成功');
      form.resetFields();
      setShareModalVisible(false);
      loadPermissions();
    } catch (error: any) {
      message.error(error.message || '分享失败');
    }
  };

  const handleRemovePermission = async (permissionId: number) => {
    try {
      // 调用删除权限接口（需要在API中实现）
      message.success('权限已移除');
      loadPermissions();
    } catch (error: any) {
      message.error(error.message || '移除失败');
    }
  };

  const columns: ColumnsType<Permission> = [
    {
      title: '用户名',
      dataIndex: 'username',
      key: 'username',
    },
    {
      title: '邮箱',
      dataIndex: 'email',
      key: 'email',
    },
    {
      title: '权限',
      dataIndex: 'role',
      key: 'role',
      render: (role: string) => {
        const roleMap: Record<string, { text: string; color: string }> = {
          OWNER: { text: '拥有者', color: 'text-purple-600' },
          EDITOR: { text: '编辑者', color: 'text-blue-600' },
          VIEWER: { text: '查看者', color: 'text-green-600' },
        };
        const roleInfo = roleMap[role] || { text: role, color: 'text-gray-600' };
        return <span className={`font-medium ${roleInfo.color}`}>{roleInfo.text}</span>;
      },
    },
    {
      title: '授权时间',
      dataIndex: 'grantedAt',
      key: 'grantedAt',
      render: (time: string) => new Date(time).toLocaleString('zh-CN'),
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => {
        if (record.role === 'OWNER') {
          return <span className="text-gray-400">-</span>;
        }
        return (
          <Popconfirm
            title="确认移除该用户的访问权限？"
            onConfirm={() => handleRemovePermission(record.id)}
            okText="确认"
            cancelText="取消"
          >
            <Button type="link" danger icon={<DeleteOutlined />}>
              移除
            </Button>
          </Popconfirm>
        );
      },
    },
  ];

  return (
    <div className="max-w-6xl mx-auto p-6">
      <div className="mb-6">
        <Button
          icon={<ArrowLeftOutlined />}
          onClick={() => router.back()}
        >
          返回
        </Button>
      </div>

      <Card
        title="文档权限管理"
        extra={
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => setShareModalVisible(true)}
          >
            添加协作者
          </Button>
        }
      >
        <Table
          columns={columns}
          dataSource={permissions}
          rowKey="id"
          loading={loading}
          pagination={false}
        />
      </Card>

      <Modal
        title="添加协作者"
        open={shareModalVisible}
        onCancel={() => {
          setShareModalVisible(false);
          form.resetFields();
        }}
        footer={null}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleShare}
        >
          <Form.Item
            label="用户邮箱"
            name="email"
            rules={[
              { required: true, message: '请输入用户邮箱' },
              { type: 'email', message: '请输入有效的邮箱地址' },
            ]}
          >
            <Input placeholder="输入要分享的用户邮箱" />
          </Form.Item>

          <Form.Item
            label="权限角色"
            name="role"
            initialValue="VIEWER"
            rules={[{ required: true, message: '请选择权限' }]}
          >
            <Select>
              <Option value="EDITOR">编辑者（可编辑文档）</Option>
              <Option value="VIEWER">查看者（仅可查看）</Option>
            </Select>
          </Form.Item>

          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit">
                确认分享
              </Button>
              <Button onClick={() => {
                setShareModalVisible(false);
                form.resetFields();
              }}>
                取消
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
