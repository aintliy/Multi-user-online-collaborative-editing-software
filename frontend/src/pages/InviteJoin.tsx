import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Result, Button, Spin, message } from 'antd';
import { collaboratorApi } from '../api';
import { useAuthStore } from '../store/useAuthStore';

const InviteJoin: React.FC = () => {
  const { token: inviteToken } = useParams<{ token: string }>();
  const navigate = useNavigate();
  const { token: authToken } = useAuthStore();
  const [loading, setLoading] = useState(true);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!authToken) {
      // 未登录，跳转登录页
      message.warning('请先登录后再加入协作');
      navigate(`/login?redirect=/invite/${inviteToken}`);
      return;
    }

    handleJoin();
  }, [authToken, inviteToken]);

  const handleJoin = async () => {
    try {
      await collaboratorApi.joinByInvite(inviteToken!);
      setSuccess(true);
    } catch (err: any) {
      setError(err.response?.data?.message || '加入失败');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <Spin size="large" tip="正在加入协作..." />
      </div>
    );
  }

  if (success) {
    return (
      <Result
        status="success"
        title="加入成功"
        subTitle="您已成功加入文档协作"
        extra={
          <Button type="primary" onClick={() => navigate('/documents')}>
            前往我的文档
          </Button>
        }
      />
    );
  }

  return (
    <Result
      status="error"
      title="加入失败"
      subTitle={error || '邀请链接无效或已过期'}
      extra={
        <Button type="primary" onClick={() => navigate('/documents')}>
          返回首页
        </Button>
      }
    />
  );
};

export default InviteJoin;
