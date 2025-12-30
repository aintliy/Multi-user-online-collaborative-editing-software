import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, List, Input, Empty, Button, message } from 'antd';
import {
  FileTextOutlined,
  GlobalOutlined,
  UserOutlined,
  ClockCircleOutlined,
  CopyOutlined,
} from '@ant-design/icons';
import { documentApi, collaboratorApi } from '../api';
import type { Document } from '../types';
import { useAuthStore } from '../store/useAuthStore';
import dayjs from 'dayjs';
import './PublicDocuments.scss';

const { Search } = Input;

const PublicDocuments: React.FC = () => {
  const navigate = useNavigate();
  const { token } = useAuthStore();
  const [documents, setDocuments] = useState<Document[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchKeyword, setSearchKeyword] = useState('');

  useEffect(() => {
    fetchPublicDocuments();
  }, [searchKeyword]);

  const fetchPublicDocuments = async () => {
    setLoading(true);
    try {
      const data = await documentApi.searchPublic({
        keyword: searchKeyword || undefined,
      });
      setDocuments(data.content);
    } catch (error) {
      console.error('Failed to fetch public documents:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleClone = async (doc: Document) => {
    if (!token) {
      message.warning('请先登录');
      navigate('/login');
      return;
    }
    try {
      const newDoc = await documentApi.clone(doc.id, { title: `${doc.title} - 副本` });
      message.success('克隆成功');
      navigate(`/documents/${newDoc.id}`);
    } catch (error: any) {
      message.error(error.response?.data?.message || '克隆失败');
    }
  };

  const handleRequestAccess = async (doc: Document) => {
    if (!token) {
      message.warning('请先登录');
      navigate('/login');
      return;
    }
    try {
      await collaboratorApi.submitRequest(doc.id);
      message.success('协作申请已提交');
    } catch (error: any) {
      message.error(error.response?.data?.message || '申请失败');
    }
  };

  return (
    <div className="public-documents-page">
      <Card
        title={
          <span>
            <GlobalOutlined /> 公开文档
          </span>
        }
        extra={
          token ? (
            <Button onClick={() => navigate('/documents')}>我的文档</Button>
          ) : (
            <Button type="primary" onClick={() => navigate('/login')}>登录</Button>
          )
        }
      >
        <div className="search-section">
          <Search
            placeholder="搜索公开文档..."
            allowClear
            enterButton
            size="large"
            onSearch={setSearchKeyword}
            style={{ maxWidth: 500 }}
          />
        </div>

        {!documents || documents.length === 0 ? (
          <Empty description="暂无公开文档" style={{ marginTop: 60 }} />
        ) : (
          <List
            loading={loading}
            grid={{ gutter: 16, xs: 1, sm: 2, md: 2, lg: 3, xl: 3, xxl: 4 }}
            dataSource={documents}
            renderItem={(doc) => (
              <List.Item>
                <Card
                  className="document-card"
                  hoverable
                  actions={[
                    <Button
                      type="text"
                      icon={<CopyOutlined />}
                      onClick={() => handleClone(doc)}
                    >
                      克隆
                    </Button>,
                    <Button
                      type="text"
                      onClick={() => handleRequestAccess(doc)}
                    >
                      申请协作
                    </Button>,
                  ]}
                >
                  <Card.Meta
                    avatar={<FileTextOutlined className="doc-icon" />}
                    title={doc.title}
                    description={
                      <div className="doc-info">
                        <div className="doc-owner">
                          <UserOutlined /> {doc.owner?.username}
                        </div>
                        <div className="doc-time">
                          <ClockCircleOutlined /> {dayjs(doc.updatedAt).format('YYYY-MM-DD')}
                        </div>
                      </div>
                    }
                  />
                </Card>
              </List.Item>
            )}
          />
        )}
      </Card>
    </div>
  );
};

export default PublicDocuments;
