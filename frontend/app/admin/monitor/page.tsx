'use client';

import React, { useState, useEffect } from 'react';
import { Card, Row, Col, Statistic, Table, Select, DatePicker, Space, Tag, message, Spin } from 'antd';
import { UserOutlined, FileTextOutlined, TeamOutlined, FundOutlined, DatabaseOutlined, CheckCircleOutlined, CloseCircleOutlined } from '@ant-design/icons';
import { getSystemStats, getOperationLogs, getHealthStatus, SystemStats, OperationLog, HealthStatus } from '@/lib/api/monitor';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';

const { RangePicker } = DatePicker;
const { Option } = Select;

export default function MonitorPage() {
  const [stats, setStats] = useState<SystemStats | null>(null);
  const [health, setHealth] = useState<HealthStatus | null>(null);
  const [logs, setLogs] = useState<OperationLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [logsLoading, setLogsLoading] = useState(false);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 20,
    total: 0,
  });
  const [filters, setFilters] = useState<{
    action?: string;
    targetType?: string;
    dateRange?: [string, string];
  }>({});

  // 加载系统统计和健康状态
  useEffect(() => {
    loadSystemData();
    // 每30秒刷新一次
    const interval = setInterval(loadSystemData, 30000);
    return () => clearInterval(interval);
  }, []);

  // 加载操作日志
  useEffect(() => {
    loadOperationLogs();
  }, [pagination.current, pagination.pageSize, filters]);

  const loadSystemData = async () => {
    try {
      const [statsData, healthData] = await Promise.all([
        getSystemStats(),
        getHealthStatus(),
      ]);
      setStats(statsData);
      setHealth(healthData);
    } catch (error: any) {
      message.error(error.message || '加载系统数据失败');
    } finally {
      setLoading(false);
    }
  };

  const loadOperationLogs = async () => {
    setLogsLoading(true);
    try {
      const response = await getOperationLogs({
        page: pagination.current,
        size: pagination.pageSize,
        action: filters.action,
        targetType: filters.targetType,
        startDate: filters.dateRange?.[0],
        endDate: filters.dateRange?.[1],
      });
      setLogs(response.records);
      setPagination(prev => ({
        ...prev,
        total: response.total,
      }));
    } catch (error: any) {
      message.error(error.message || '加载操作日志失败');
    } finally {
      setLogsLoading(false);
    }
  };

  const handleTableChange = (newPagination: any) => {
    setPagination({
      current: newPagination.current,
      pageSize: newPagination.pageSize,
      total: pagination.total,
    });
  };

  const handleFilterChange = (key: string, value: any) => {
    setFilters(prev => ({
      ...prev,
      [key]: value,
    }));
    setPagination(prev => ({ ...prev, current: 1 }));
  };

  const getActionTag = (action: string) => {
    const colorMap: Record<string, string> = {
      CREATE_DOC: 'green',
      DELETE_DOC: 'red',
      UPDATE_PERMISSION: 'blue',
      UPDATE_DOC: 'orange',
    };
    return <Tag color={colorMap[action] || 'default'}>{action}</Tag>;
  };

  const getTargetTypeTag = (type: string) => {
    const colorMap: Record<string, string> = {
      DOC: 'blue',
      USER: 'green',
      ROLE: 'purple',
      PERMISSION: 'orange',
    };
    return <Tag color={colorMap[type] || 'default'}>{type}</Tag>;
  };

  const columns: ColumnsType<OperationLog> = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
    },
    {
      title: '用户ID',
      dataIndex: 'userId',
      key: 'userId',
      width: 100,
    },
    {
      title: '操作类型',
      dataIndex: 'action',
      key: 'action',
      width: 150,
      render: (action: string) => getActionTag(action),
    },
    {
      title: '目标类型',
      dataIndex: 'targetType',
      key: 'targetType',
      width: 120,
      render: (type: string) => getTargetTypeTag(type),
    },
    {
      title: '目标ID',
      dataIndex: 'targetId',
      key: 'targetId',
      width: 100,
    },
    {
      title: '详情',
      dataIndex: 'detail',
      key: 'detail',
      ellipsis: true,
    },
    {
      title: '操作时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
      render: (time: string) => dayjs(time).format('YYYY-MM-DD HH:mm:ss'),
    },
  ];

  if (loading) {
    return (
      <div className="flex justify-center items-center h-screen">
        <Spin size="large" />
      </div>
    );
  }

  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold mb-6">系统监控</h1>

      {/* 系统统计 */}
      <Row gutter={16} className="mb-6">
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="总用户数"
              value={stats?.totalUsers || 0}
              prefix={<UserOutlined />}
              valueStyle={{ color: '#3f8600' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="总文档数"
              value={stats?.totalDocuments || 0}
              prefix={<FileTextOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="在线用户"
              value={stats?.onlineUsers || 0}
              prefix={<TeamOutlined />}
              valueStyle={{ color: '#cf1322' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="活跃文档"
              value={stats?.activeDocuments || 0}
              prefix={<FundOutlined />}
              valueStyle={{ color: '#faad14' }}
            />
          </Card>
        </Col>
      </Row>

      {/* 系统健康状态 */}
      <Card title="系统健康状态" className="mb-6">
        <Row gutter={16}>
          <Col xs={24} sm={12}>
            <div className="flex items-center mb-4">
              <DatabaseOutlined className="text-2xl mr-3" />
              <div>
                <div className="font-medium">数据库状态</div>
                <div className="flex items-center mt-1">
                  {health?.database === 'UP' ? (
                    <>
                      <CheckCircleOutlined className="text-green-600 mr-2" />
                      <span className="text-green-600">正常</span>
                    </>
                  ) : (
                    <>
                      <CloseCircleOutlined className="text-red-600 mr-2" />
                      <span className="text-red-600">异常</span>
                    </>
                  )}
                </div>
                {health?.databaseError && (
                  <div className="text-red-500 text-sm mt-1">{health.databaseError}</div>
                )}
              </div>
            </div>
          </Col>
          <Col xs={24} sm={12}>
            <div className="flex items-center">
              <FundOutlined className="text-2xl mr-3" />
              <div>
                <div className="font-medium">内存使用</div>
                <div className="text-sm text-gray-600 mt-1">
                  <div>总计: {health?.memory?.total}</div>
                  <div>已用: {health?.memory?.used}</div>
                  <div>可用: {health?.memory?.free}</div>
                </div>
              </div>
            </div>
          </Col>
        </Row>
      </Card>

      {/* 操作日志 */}
      <Card title="操作日志">
        <Space className="mb-4" wrap>
          <Select
            placeholder="操作类型"
            style={{ width: 150 }}
            allowClear
            onChange={(value) => handleFilterChange('action', value)}
          >
            <Option value="CREATE_DOC">创建文档</Option>
            <Option value="DELETE_DOC">删除文档</Option>
            <Option value="UPDATE_PERMISSION">更新权限</Option>
            <Option value="UPDATE_DOC">更新文档</Option>
          </Select>

          <Select
            placeholder="目标类型"
            style={{ width: 150 }}
            allowClear
            onChange={(value) => handleFilterChange('targetType', value)}
          >
            <Option value="DOC">文档</Option>
            <Option value="USER">用户</Option>
            <Option value="ROLE">角色</Option>
            <Option value="PERMISSION">权限</Option>
          </Select>

          <RangePicker
            onChange={(dates) => {
              if (dates) {
                handleFilterChange('dateRange', [
                  dates[0]?.format('YYYY-MM-DD'),
                  dates[1]?.format('YYYY-MM-DD'),
                ]);
              } else {
                handleFilterChange('dateRange', undefined);
              }
            }}
          />
        </Space>

        <Table
          columns={columns}
          dataSource={logs}
          rowKey="id"
          loading={logsLoading}
          pagination={pagination}
          onChange={handleTableChange}
          scroll={{ x: 1000 }}
        />
      </Card>
    </div>
  );
}
