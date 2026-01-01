import React, { useEffect, useState } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import {
  Layout,
  Menu,
  Avatar,
  Dropdown,
  Badge,
  Space,
  message,
} from 'antd';
import {
  FileTextOutlined,
  UserOutlined,
  TeamOutlined,
  BellOutlined,
  LogoutOutlined,
  SettingOutlined,
  GlobalOutlined,
} from '@ant-design/icons';
import { useAuthStore } from '../store/useAuthStore';
import { notificationApi } from '../api';
import wsService from '../utils/websocket';
import './MainLayout.scss';

const { Header, Sider, Content } = Layout;

const MainLayout: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { user, token, logout } = useAuthStore();
  const [collapsed, setCollapsed] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);

  useEffect(() => {
    // 连接 WebSocket
    if (token) {
      wsService.connect(token).then(() => {
        wsService.subscribeToNotifications(user!.id, () => {
          fetchUnreadCount();
        });
      }).catch((err) => {
        console.error('WebSocket connection failed:', err);
      });
    }

    return () => {
      wsService.disconnect();
    };
  }, [token]);

  useEffect(() => {
    fetchUnreadCount();

    const handleLocalRead = () => fetchUnreadCount();
    window.addEventListener('notification-read', handleLocalRead);
    return () => window.removeEventListener('notification-read', handleLocalRead);
  }, []);

  const fetchUnreadCount = async () => {
    try {
      const res = await notificationApi.getUnreadCount();
      setUnreadCount(res.count);
    } catch (error) {
      // ignore
    }
  };

  const handleLogout = () => {
    logout();
    message.success('已退出登录');
    navigate('/login');
  };

  const menuItems = [
    {
      key: '/documents',
      icon: <FileTextOutlined />,
      label: '我的文档',
    },
    {
      key: '/public',
      icon: <GlobalOutlined />,
      label: '公开文档',
    },
    {
      key: '/friends',
      icon: <TeamOutlined />,
      label: '好友',
    },
    {
      key: '/notifications',
      icon: <Badge count={unreadCount} size="small"><BellOutlined /></Badge>,
      label: '通知',
    },
  ];

  const userMenuItems = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: '个人中心',
      onClick: () => navigate('/profile'),
    },
    ...(user?.role === 'ADMIN' ? [{
      key: 'admin',
      icon: <SettingOutlined />,
      label: '管理后台',
      onClick: () => navigate('/admin'),
    }] : []),
    {
      type: 'divider' as const,
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      onClick: handleLogout,
    },
  ];

  const selectedKey = menuItems.find(item => 
    location.pathname.startsWith(item.key)
  )?.key || '/documents';

  return (
    <Layout className="main-layout">
      <Sider
        collapsible
        collapsed={collapsed}
        onCollapse={setCollapsed}
        theme="light"
        className="main-sider"
      >
        <div className="logo">
          {collapsed ? 'CD' : 'CollabDoc'}
        </div>
        <Menu
          mode="inline"
          selectedKeys={[selectedKey]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
        />
      </Sider>
      <Layout>
        <Header className="main-header">
          <div className="header-content">
            <div className="header-title">
              {/* Can add breadcrumb or title here */}
            </div>
            <div className="header-actions">
              <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
                <Space className="user-info" style={{ cursor: 'pointer' }}>
                  <Avatar 
                    src={user?.avatarUrl ? (user.avatarUrl.startsWith('http') ? user.avatarUrl : `http://localhost:8080${user.avatarUrl}`) : undefined} 
                    icon={<UserOutlined />} 
                  />
                  <span className="username">{user?.username}</span>
                </Space>
              </Dropdown>
            </div>
          </div>
        </Header>
        <Content className="main-content">
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
};

export default MainLayout;
