'use client';

import React, { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Button, Table, Space, Modal, Form, Input, message, Tag } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, ShareAltOutlined } from '@ant-design/icons';
import { useAuth } from '@/contexts/AuthContext';
import {
  getDocuments,
  createDocument,
  deleteDocument,
  Document,
  CreateDocumentRequest,
  PageResult,
} from '@/lib/api/document';

export default function DocumentsPage() {
  const router = useRouter();
  const { user } = useAuth();
  const [documents, setDocuments] = useState<Document[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize] = useState(10);
  const [createModalVisible, setCreateModalVisible] = useState(false);
  const [form] = Form.useForm();

  useEffect(() => {
    fetchDocuments();
  }, [currentPage]);

  const fetchDocuments = async () => {
    setLoading(true);
    try {
      const result: PageResult<Document> = await getDocuments(currentPage, pageSize);
      setDocuments(result.records);
      setTotal(result.total);
    } catch (error: any) {
      message.error('获取文档列表失败');
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = async (values: CreateDocumentRequest) => {
    try {
      await createDocument(values);
      message.success('文档创建成功');
      setCreateModalVisible(false);
      form.resetFields();
      fetchDocuments();
    } catch (error: any) {
      message.error(error.message || '创建失败');
    }
  };

  const handleDelete = (id: number) => {
    Modal.confirm({
      title: '确认删除',
      content: '确定要删除这个文档吗？此操作不可恢复。',
      onOk: async () => {
        try {
          await deleteDocument(id);
          message.success('删除成功');
          fetchDocuments();
        } catch (error: any) {
          message.error(error.message || '删除失败');
        }
      },
    });
  };

  const columns = [
    {
      title: '标题',
      dataIndex: 'title',
      key: 'title',
      render: (text: string, record: Document) => (
        <a onClick={() => router.push(`/documents/${record.id}`)}>
          {text}
        </a>
      ),
    },
    {
      title: '所有者',
      dataIndex: 'ownerName',
      key: 'ownerName',
    },
    {
      title: '权限',
      dataIndex: 'permission',
      key: 'permission',
      render: (permission: string) => {
        const colorMap: Record<string, string> = {
          OWNER: 'blue',
          EDITOR: 'green',
          VIEWER: 'orange',
        };
        return <Tag color={colorMap[permission] || 'default'}>{permission}</Tag>;
      },
    },
    {
      title: '更新时间',
      dataIndex: 'updatedAt',
      key: 'updatedAt',
      render: (date: string) => new Date(date).toLocaleString('zh-CN'),
    },
    {
      title: '操作',
      key: 'action',
      render: (_: any, record: Document) => (
        <Space>
          <Button
            type="link"
            icon={<EditOutlined />}
            onClick={() => router.push(`/documents/${record.id}`)}
          >
            编辑
          </Button>
          {record.permission === 'OWNER' && (
            <>
              <Button
                type="link"
                icon={<ShareAltOutlined />}
                onClick={() => router.push(`/documents/${record.id}/share`)}
              >
                分享
              </Button>
              <Button
                type="link"
                danger
                icon={<DeleteOutlined />}
                onClick={() => handleDelete(record.id)}
              >
                删除
              </Button>
            </>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-7xl mx-auto py-8 px-4">
        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex justify-between items-center mb-6">
            <h1 className="text-2xl font-bold text-gray-800">我的文档</h1>
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => setCreateModalVisible(true)}
            >
              新建文档
            </Button>
          </div>

          <Table
            columns={columns}
            dataSource={documents}
            rowKey="id"
            loading={loading}
            pagination={{
              current: currentPage,
              pageSize: pageSize,
              total: total,
              onChange: (page) => setCurrentPage(page),
            }}
          />
        </div>
      </div>

      <Modal
        title="创建文档"
        open={createModalVisible}
        onCancel={() => {
          setCreateModalVisible(false);
          form.resetFields();
        }}
        footer={null}
      >
        <Form
          form={form}
          onFinish={handleCreate}
          layout="vertical"
        >
          <Form.Item
            name="title"
            label="文档标题"
            rules={[{ required: true, message: '请输入文档标题' }]}
          >
            <Input placeholder="请输入文档标题" />
          </Form.Item>

          <Form.Item
            name="docType"
            label="文档类型"
            initialValue="doc"
          >
            <Input disabled />
          </Form.Item>

          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit">
                创建
              </Button>
              <Button onClick={() => {
                setCreateModalVisible(false);
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
