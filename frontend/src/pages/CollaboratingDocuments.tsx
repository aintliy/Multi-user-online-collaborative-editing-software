import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, List, Empty, Spin, Avatar } from 'antd';
import {
  FileTextOutlined,
  TeamOutlined,
  UserOutlined,
  ClockCircleOutlined,
} from '@ant-design/icons';
import { documentApi } from '../api';
import type { Document } from '../types';
import { getAvatarUrl } from '../utils/request';
import dayjs from 'dayjs';
import './CollaboratingDocuments.scss';

const CollaboratingDocuments: React.FC = () => {
  const navigate = useNavigate();
  const [documents, setDocuments] = useState<Document[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchCollaboratingDocuments();
  }, []);

  const fetchCollaboratingDocuments = async () => {
    setLoading(true);
    try {
      const data = await documentApi.getCollaborating();
      setDocuments(data);
    } catch (error) {
      console.error('Failed to fetch collaborating documents:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="collaborating-documents-page">
      <Card
        title={
          <span>
            <TeamOutlined /> 协作文档
          </span>
        }
      >
        {loading ? (
          <div className="loading-container">
            <Spin size="large" />
          </div>
        ) : !documents || documents.length === 0 ? (
          <Empty 
            description="暂无协作文档" 
            style={{ marginTop: 60 }}
          >
            <p style={{ color: '#999' }}>
              当其他用户邀请您协作编辑文档时，文档将显示在这里
            </p>
          </Empty>
        ) : (
          <List
            grid={{ gutter: 16, xs: 1, sm: 2, md: 2, lg: 3, xl: 3, xxl: 4 }}
            dataSource={documents}
            renderItem={(doc) => (
              <List.Item>
                <Card
                  className="document-card"
                  hoverable
                  onClick={() => navigate(`/documents/${doc.id}`)}
                >
                  <Card.Meta
                    avatar={<FileTextOutlined className="doc-icon" />}
                    title={doc.title}
                    description={
                      <div className="doc-info">
                        <div className="doc-owner">
                          <Avatar 
                            size="small" 
                            src={getAvatarUrl(doc.ownerAvatarUrl)} 
                            icon={<UserOutlined />}
                            style={{ marginRight: 4 }}
                          />
                          {doc.ownerName || '未知用户'}
                        </div>
                        <div className="doc-time">
                          <ClockCircleOutlined /> {dayjs(doc.updatedAt).format('YYYY-MM-DD HH:mm')}
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

export default CollaboratingDocuments;
