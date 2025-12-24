import React from 'react';
import { Outlet } from 'react-router-dom';
import { Layout } from 'antd';
import './AuthLayout.scss';

const { Content } = Layout;

const AuthLayout: React.FC = () => {
  return (
    <Layout className="auth-layout">
      <Content className="auth-content">
        <div className="auth-container">
          <div className="auth-header">
            <h1 className="auth-logo">CollabDoc</h1>
            <p className="auth-subtitle">多人在线协作编辑平台</p>
          </div>
          <div className="auth-form-container">
            <Outlet />
          </div>
        </div>
      </Content>
    </Layout>
  );
};

export default AuthLayout;
