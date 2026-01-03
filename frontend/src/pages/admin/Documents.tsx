import React, { useEffect, useState } from 'react';
import {
  Table,
  Card,
  Input,
  Button,
  Tag,
  Space,
  Modal,
  message,
} from 'antd';
import { DeleteOutlined, EyeOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { adminApi } from '../../api';
import type { Document } from '../../types';
import dayjs from 'dayjs';

const { Search } = Input;

const Documents: React.FC = () => {
  const [documents, setDocuments] = useState<Document[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [keyword, setKeyword] = useState('');

  useEffect(() => {
    fetchDocuments();
  }, [page, pageSize, keyword]);

  const fetchDocuments = async () => {
    setLoading(true);
    try {
      const data = await adminApi.getDocuments({
        keyword: keyword || undefined,
        page,
        pageSize,
      });
      setDocuments(data.items);
      setTotal(data.total);
    } catch (error) {
      console.error('Failed to fetch documents:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteDocument = async (documentId: number) => {
    Modal.confirm({
      title: '确认删除',
      content: '确定要删除该文档吗？此操作不可恢复。',
      okType: 'danger',
      onOk: async () => {
        try {
          await adminApi.deleteDocument(documentId);
          message.success('文档已删除');
          fetchDocuments();
        } catch (error: any) {
          message.error(error.response?.data?.message || '删除失败');
        }
      },
    });
  };

  const columns: ColumnsType<Document> = [
    {
      title: '标题',
      dataIndex: 'title',
      ellipsis: true,
    },
    {
      title: '所有者',
      render: (_, record) => record.ownerName || record.owner?.username || '未知',
    },
    {
      title: '可见性',
      dataIndex: 'visibility',
      render: (visibility: string) => {
        const isPublic = visibility === 'PUBLIC';
        return (
          <Tag color={isPublic ? 'blue' : 'default'}>
            {isPublic ? '公开' : '私有'}
          </Tag>
        );
      },
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      render: (date: string) => dayjs(date).format('YYYY-MM-DD HH:mm'),
    },
    {
      title: '更新时间',
      dataIndex: 'updatedAt',
      render: (date: string) => dayjs(date).format('YYYY-MM-DD HH:mm'),
    },
    {
      title: '操作',
      key: 'actions',
      render: (_, record) => (
        <Space>
          <Button
            size="small"
            icon={<EyeOutlined />}
            onClick={() => window.open(`/documents/${record.id}`, '_blank')}
          >
            查看
          </Button>
          <Button
            size="small"
            danger
            icon={<DeleteOutlined />}
            onClick={() => handleDeleteDocument(record.id)}
          >
            删除
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <Card title="文档管理">
      <Space style={{ marginBottom: 16 }}>
        <Search
          placeholder="搜索文档标题"
          allowClear
          onSearch={setKeyword}
          style={{ width: 240 }}
        />
      </Space>

      <Table
        columns={columns}
        dataSource={documents}
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

export default Documents;
