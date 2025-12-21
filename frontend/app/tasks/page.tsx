'use client';

import React, { useEffect, useState } from 'react';
import {
  Card,
  Table,
  Space,
  Button,
  Modal,
  Form,
  Input,
  Select,
  DatePicker,
  message,
  Tag,
  Typography,
} from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, CheckOutlined } from '@ant-design/icons';
import { getMyTasks, createTask, updateTask, deleteTask, TaskVO } from '@/lib/api/task';
import { getAllUsers, UserVO, PageResult } from '@/lib/api/admin';
import dayjs from 'dayjs';

const { Title } = Typography;
const { Option } = Select;

export default function TasksPage() {
  const [tasks, setTasks] = useState<TaskVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingTask, setEditingTask] = useState<TaskVO | null>(null);
  const [statusFilter, setStatusFilter] = useState<string | undefined>(undefined);
  const [users, setUsers] = useState<UserVO[]>([]);
  const [form] = Form.useForm();

  useEffect(() => {
    fetchTasks();
    fetchUsers();
  }, [statusFilter]);

  const fetchTasks = async () => {
    setLoading(true);
    try {
      const data = await getMyTasks(statusFilter);
      setTasks(data);
    } catch (error: any) {
      message.error('获取任务列表失败');
    } finally {
      setLoading(false);
    }
  };

  const fetchUsers = async () => {
    try {
      const result: PageResult<UserVO> = await getAllUsers(1, 100);
      setUsers(result.records);
    } catch (error) {
      console.error('Failed to fetch users:', error);
    }
  };

  const handleCreate = () => {
    setEditingTask(null);
    form.resetFields();
    setModalVisible(true);
  };

  const handleEdit = (task: TaskVO) => {
    setEditingTask(task);
    form.setFieldsValue({
      ...task,
      dueDate: task.dueDate ? dayjs(task.dueDate) : undefined,
    });
    setModalVisible(true);
  };

  const handleSubmit = async (values: any) => {
    try {
      const data = {
        ...values,
        dueDate: values.dueDate ? values.dueDate.format('YYYY-MM-DD HH:mm:ss') : undefined,
      };

      if (editingTask) {
        await updateTask(editingTask.id, data);
        message.success('任务更新成功');
      } else {
        await createTask(data);
        message.success('任务创建成功');
      }

      setModalVisible(false);
      form.resetFields();
      fetchTasks();
    } catch (error: any) {
      message.error(error.message || '操作失败');
    }
  };

  const handleDelete = (id: number) => {
    Modal.confirm({
      title: '确认删除',
      content: '确定要删除这个任务吗？',
      onOk: async () => {
        try {
          await deleteTask(id);
          message.success('删除成功');
          fetchTasks();
        } catch (error: any) {
          message.error(error.message || '删除失败');
        }
      },
    });
  };

  const handleUpdateStatus = async (id: number, status: 'TODO' | 'DOING' | 'DONE') => {
    try {
      await updateTask(id, { status });
      message.success('状态更新成功');
      fetchTasks();
    } catch (error: any) {
      message.error(error.message || '更新失败');
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'TODO':
        return 'default';
      case 'DOING':
        return 'processing';
      case 'DONE':
        return 'success';
      default:
        return 'default';
    }
  };

  const getStatusText = (status: string) => {
    switch (status) {
      case 'TODO':
        return '待办';
      case 'DOING':
        return '进行中';
      case 'DONE':
        return '已完成';
      default:
        return status;
    }
  };

  const columns = [
    {
      title: '标题',
      dataIndex: 'title',
      key: 'title',
      width: 200,
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
    },
    {
      title: '负责人',
      dataIndex: 'assigneeName',
      key: 'assigneeName',
      width: 100,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: string) => (
        <Tag color={getStatusColor(status)}>{getStatusText(status)}</Tag>
      ),
    },
    {
      title: '截止日期',
      dataIndex: 'dueDate',
      key: 'dueDate',
      width: 180,
      render: (date: string) => (date ? dayjs(date).format('YYYY-MM-DD HH:mm') : '-'),
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      render: (_: any, record: TaskVO) => (
        <Space>
          {record.status !== 'DONE' && (
            <Button
              size="small"
              icon={<CheckOutlined />}
              onClick={() =>
                handleUpdateStatus(
                  record.id,
                  record.status === 'TODO' ? 'DOING' : 'DONE'
                )
              }
            >
              {record.status === 'TODO' ? '开始' : '完成'}
            </Button>
          )}
          <Button
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          />
          <Button
            size="small"
            danger
            icon={<DeleteOutlined />}
            onClick={() => handleDelete(record.id)}
          />
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Card>
        <Space direction="vertical" size="large" style={{ width: '100%' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Title level={3} style={{ margin: 0 }}>
              我的任务
            </Title>
            <Space>
              <Select
                style={{ width: 120 }}
                placeholder="筛选状态"
                allowClear
                value={statusFilter}
                onChange={setStatusFilter}
              >
                <Option value="TODO">待办</Option>
                <Option value="DOING">进行中</Option>
                <Option value="DONE">已完成</Option>
              </Select>
              <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
                创建任务
              </Button>
            </Space>
          </div>

          <Table
            columns={columns}
            dataSource={tasks}
            rowKey="id"
            loading={loading}
            pagination={false}
          />
        </Space>
      </Card>

      {/* 创建/编辑任务对话框 */}
      <Modal
        title={editingTask ? '编辑任务' : '创建任务'}
        open={modalVisible}
        onCancel={() => {
          setModalVisible(false);
          form.resetFields();
        }}
        onOk={() => form.submit()}
        width={600}
      >
        <Form form={form} layout="vertical" onFinish={handleSubmit}>
          <Form.Item
            label="任务标题"
            name="title"
            rules={[{ required: true, message: '请输入任务标题' }]}
          >
            <Input placeholder="输入任务标题" />
          </Form.Item>

          <Form.Item
            label="任务描述"
            name="description"
            rules={[{ required: true, message: '请输入任务描述' }]}
          >
            <Input.TextArea rows={4} placeholder="输入任务描述" />
          </Form.Item>

          <Form.Item
            label="负责人"
            name="assigneeId"
            rules={[{ required: true, message: '请选择负责人' }]}
          >
            <Select placeholder="选择负责人">
              {users.map((user) => (
                <Option key={user.id} value={user.id}>
                  {user.username} ({user.email})
                </Option>
              ))}
            </Select>
          </Form.Item>

          {editingTask && (
            <Form.Item label="状态" name="status">
              <Select>
                <Option value="TODO">待办</Option>
                <Option value="DOING">进行中</Option>
                <Option value="DONE">已完成</Option>
              </Select>
            </Form.Item>
          )}

          <Form.Item label="截止日期" name="dueDate">
            <DatePicker showTime style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item label="关联文档" name="documentId">
            <Input placeholder="文档ID（可选）" type="number" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
