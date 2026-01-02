import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Card,
  List,
  Avatar,
  Tag,
  Button,
  Empty,
  Spin,
  message,
  Typography,
  Space,
} from 'antd';
import {
  ArrowLeftOutlined,
  FileTextOutlined,
  UserOutlined,
  EyeOutlined,
} from '@ant-design/icons';
import { userApi } from '../api';
import type { User, Document } from '../types';
import { getAvatarUrl } from '../utils/request';
import dayjs from 'dayjs';
import './UserRepos.scss';

const { Title, Text } = Typography;

const UserRepos: React.FC = () => {
  const { publicId } = useParams<{ publicId: string }>();
  const navigate = useNavigate();
  const [user, setUser] = useState<User | null>(null);
  const [documents, setDocuments] = useState<Document[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (publicId) {
      fetchUserRepos();
    }
  }, [publicId]);

  const fetchUserRepos = async () => {
    setLoading(true);
    try {
      const data = await userApi.getUserPublicDocs(publicId!);
      setUser(data.user);
      setDocuments(data.documents);
    } catch (error: any) {
      message.error(error.response?.data?.message || '获取用户仓库失败');
    } finally {
      setLoading(false);
    }
  };

  const handleViewDocument = (docId: number) => {
    navigate(`/documents/${docId}`);
  };

  if (loading) {
    return (
      <div className="user-repos-loading">
        <Spin size="large" />
      </div>
    );
  }

  return (
    <div className="user-repos-page">
      <Card>
        <Button
          type="text"
          icon={<ArrowLeftOutlined />}
          onClick={() => navigate(-1)}
          style={{ marginBottom: 16 }}
        >
          返回
        </Button>

        {user && (
          <div className="user-header">
            <Avatar
              size={64}
              src={getAvatarUrl(user.avatarUrl)}
              icon={<UserOutlined />}
            />
            <div className="user-info">
              <Title level={4}>{user.username}</Title>
              <Text type="secondary">@{user.publicId}</Text>
              {user.profile && <Text className="user-profile">{user.profile}</Text>}
            </div>
          </div>
        )}

        <Title level={5} style={{ marginTop: 24 }}>
          公开文档 ({documents.length})
        </Title>

        <List
          dataSource={documents}
          locale={{ emptyText: <Empty description="该用户暂无公开文档" /> }}
          renderItem={(doc) => (
            <List.Item
              actions={[
                <Button
                  type="primary"
                  icon={<EyeOutlined />}
                  onClick={() => handleViewDocument(doc.id)}
                >
                  查看
                </Button>,
              ]}
            >
              <List.Item.Meta
                avatar={<FileTextOutlined style={{ fontSize: 24, color: '#1890ff' }} />}
                title={
                  <Space>
                    <span style={{ cursor: 'pointer' }} onClick={() => handleViewDocument(doc.id)}>
                      {doc.title}
                    </span>
                    <Tag color="cyan">{doc.docType?.toUpperCase()}</Tag>
                  </Space>
                }
                description={
                  <Space orientation="vertical" size={0}>
                    {doc.tags && <div>标签: {doc.tags}</div>}
                    <div>更新于: {dayjs(doc.updatedAt).format('YYYY-MM-DD HH:mm')}</div>
                  </Space>
                }
              />
            </List.Item>
          )}
        />
      </Card>
    </div>
  );
};

export default UserRepos;
