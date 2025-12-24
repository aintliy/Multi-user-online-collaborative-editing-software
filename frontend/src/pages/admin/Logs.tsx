import React, { useEffect, useState } from 'react';
import {
  Table,
  Card,
  Select,
  Space,
  Tag,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { adminApi } from '../../api';
import dayjs from 'dayjs';

interface OperationLog {
  id: number;
  userId: number;
  username: string;
  operationType: string;
  targetType: string;
  targetId: number;
  description: string;
  ipAddress: string;
  createdAt: string;
}

const Logs: React.FC = () => {
  const [logs, setLogs] = useState<OperationLog[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [operationType, setOperationType] = useState<string | undefined>();

  useEffect(() => {
    fetchLogs();
  }, [page, pageSize, operationType]);

  const fetchLogs = async () => {
    setLoading(true);
    try {
      const data = await adminApi.getOperationLogs({
        operationType,
        page: page - 1,
        pageSize,
      });
      setLogs(data.content);
      setTotal(data.totalElements);
    } catch (error) {
      console.error('Failed to fetch logs:', error);
    } finally {
      setLoading(false);
    }
  };

  const getOperationTypeColor = (type: string) => {
    switch (type) {
      case 'CREATE':
        return 'green';
      case 'UPDATE':
        return 'blue';
      case 'DELETE':
        return 'red';
      case 'LOGIN':
        return 'purple';
      default:
        return 'default';
    }
  };

  const columns: ColumnsType<OperationLog> = [
    {
      title: '用户',
      dataIndex: 'username',
    },
    {
      title: '操作类型',
      dataIndex: 'operationType',
      render: (type: string) => (
        <Tag color={getOperationTypeColor(type)}>{type}</Tag>
      ),
    },
    {
      title: '目标类型',
      dataIndex: 'targetType',
    },
    {
      title: '描述',
      dataIndex: 'description',
      ellipsis: true,
    },
    {
      title: 'IP地址',
      dataIndex: 'ipAddress',
    },
    {
      title: '时间',
      dataIndex: 'createdAt',
      render: (date: string) => dayjs(date).format('YYYY-MM-DD HH:mm:ss'),
    },
  ];

  return (
    <Card title="操作日志">
      <Space style={{ marginBottom: 16 }}>
        <Select
          placeholder="筛选操作类型"
          allowClear
          style={{ width: 150 }}
          onChange={setOperationType}
          options={[
            { value: 'CREATE', label: '创建' },
            { value: 'UPDATE', label: '更新' },
            { value: 'DELETE', label: '删除' },
            { value: 'LOGIN', label: '登录' },
          ]}
        />
      </Space>

      <Table
        columns={columns}
        dataSource={logs}
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

export default Logs;
