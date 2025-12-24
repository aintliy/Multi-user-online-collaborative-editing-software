import React from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { Layout, Menu } from 'antd';
import {
  DashboardOutlined,
  UserOutlined,
  FileTextOutlined,
  HistoryOutlined,
  ArrowLeftOutlined,
} from '@ant-design/icons';
import './AdminLayout.scss';

const { Header, Sider, Content } = Layout;

const AdminLayout: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();

  const menuItems = [
    {
      key: '/admin',
      icon: <DashboardOutlined />,
      label: '仪表盘',
    },
    {
      key: '/admin/users',
      icon: <UserOutlined />,
      label: '用户管理',
    },
    {
      key: '/admin/documents',
      icon: <FileTextOutlined />,
      label: '文档管理',
    },
    {
      key: '/admin/logs',
      icon: <HistoryOutlined />,
      label: '操作日志',
    },
  ];

  const selectedKey = location.pathname === '/admin' 
    ? '/admin' 
    : menuItems.find(item => item.key !== '/admin' && location.pathname.startsWith(item.key))?.key || '/admin';

  return (
    <Layout className="admin-layout">
      <Sider theme="dark" className="admin-sider">
        <div className="logo">
          管理后台
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[selectedKey]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
        />
        <div className="back-link" onClick={() => navigate('/')}>
          <ArrowLeftOutlined /> 返回主页
        </div>
      </Sider>
      <Layout>
        <Header className="admin-header">
          <span>系统管理</span>
        </Header>
        <Content className="admin-content">
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
};

export default AdminLayout;
