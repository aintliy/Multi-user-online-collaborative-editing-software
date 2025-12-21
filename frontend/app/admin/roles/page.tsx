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
  Typography,
  Descriptions,
} from 'antd';
import { EditOutlined } from '@ant-design/icons';
import {
  getAllRoles,
  getAllPermissions,
  getRolePermissions,
  updateRolePermissions,
  RoleVO,
  PermissionVO,
} from '@/lib/api/admin';

const { Title } = Typography;
const { Option } = Select;

export default function RolesPage() {
  const [roles, setRoles] = useState<RoleVO[]>([]);
  const [permissions, setPermissions] = useState<PermissionVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingRole, setEditingRole] = useState<RoleVO | null>(null);
  const [form] = Form.useForm();

  useEffect(() => {
    fetchRoles();
    fetchPermissions();
  }, []);

  const fetchRoles = async () => {
    setLoading(true);
    try {
      const data = await getAllRoles();
      setRoles(data);
    } catch (error: any) {
      message.error('获取角色列表失败');
    } finally {
      setLoading(false);
    }
  };

  const fetchPermissions = async () => {
    try {
      const data = await getAllPermissions();
      setPermissions(data);
    } catch (error) {
      console.error('Failed to fetch permissions:', error);
    }
  };

  const handleEditPermissions = async (role: RoleVO) => {
    setEditingRole(role);
    try {
      const rolePermissions = await getRolePermissions(role.id);
      form.setFieldsValue({
        permissionIds: rolePermissions.map((p) => p.id),
      });
      setModalVisible(true);
    } catch (error) {
      message.error('获取角色权限失败');
    }
  };

  const handleSubmit = async (values: any) => {
    if (!editingRole) return;

    try {
      await updateRolePermissions({
        roleId: editingRole.id,
        permissionIds: values.permissionIds,
      });
      message.success('权限更新成功');
      setModalVisible(false);
      form.resetFields();
      fetchRoles();
    } catch (error: any) {
      message.error(error.message || '更新失败');
    }
  };

  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
    },
    {
      title: '角色名称',
      dataIndex: 'name',
      key: 'name',
      width: 150,
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
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
      width: 150,
      render: (_: any, record: RoleVO) => (
        <Space>
          <Button
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEditPermissions(record)}
          >
            配置权限
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <Space direction="vertical" size="large" style={{ width: '100%' }}>
        <Card>
          <div style={{ marginBottom: 16 }}>
            <Title level={3} style={{ margin: 0 }}>
              角色管理
            </Title>
          </div>

          <Table
            columns={columns}
            dataSource={roles}
            rowKey="id"
            loading={loading}
            pagination={false}
          />
        </Card>

        <Card>
          <Title level={4}>系统权限列表</Title>
          <Descriptions column={2} bordered>
            {permissions.map((perm) => (
              <Descriptions.Item key={perm.id} label={perm.name}>
                <div>
                  <div>代码: {perm.code}</div>
                  <div style={{ color: '#666', fontSize: 12 }}>{perm.description}</div>
                </div>
              </Descriptions.Item>
            ))}
          </Descriptions>
        </Card>
      </Space>

      {/* 配置权限对话框 */}
      <Modal
        title="配置角色权限"
        open={modalVisible}
        onCancel={() => {
          setModalVisible(false);
          form.resetFields();
        }}
        onOk={() => form.submit()}
        width={600}
      >
        <Form form={form} layout="vertical" onFinish={handleSubmit}>
          <Form.Item label="角色">
            <div>
              <strong>{editingRole?.name}</strong>
              <div style={{ color: '#666', fontSize: 12 }}>{editingRole?.description}</div>
            </div>
          </Form.Item>

          <Form.Item
            label="权限"
            name="permissionIds"
            rules={[{ required: true, message: '请选择至少一个权限' }]}
          >
            <Select mode="multiple" placeholder="选择权限">
              {permissions.map((perm) => (
                <Option key={perm.id} value={perm.id}>
                  {perm.name} ({perm.code}) - {perm.description}
                </Option>
              ))}
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
