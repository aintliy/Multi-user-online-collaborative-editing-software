'use client';

import React, { useEffect } from 'react';
import { Layout, Menu, Avatar, Dropdown, Space, Typography } from 'antd';
import {
  FileTextOutlined,
  CheckSquareOutlined,
  SettingOutlined,
  LogoutOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { useRouter, usePathname } from 'next/navigation';
import { useAuth } from '@/contexts/AuthContext';
import NotificationBell from '@/components/NotificationBell';

const { Header, Sider, Content } = Layout;
const { Title } = Typography;

export default function AppLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const { user, logout } = useAuth();

  useEffect(() => {
    // 检查是否登录
    if (!user && !pathname.startsWith('/login') && !pathname.startsWith('/register')) {
      router.push('/login');
    }
  }, [user, pathname]);

  // 如果是登录/注册页面，不显示导航
  if (pathname.startsWith('/login') || pathname.startsWith('/register') || pathname === '/') {
    return <>{children}</>;
  }

  // 如果是文档编辑页面，使用特殊布局
  if (pathname.match(/^\/documents\/\d+$/)) {
    return <>{children}</>;
  }

  const handleLogout = () => {
    logout();
    router.push('/login');
  };

  const menuItems = [
    {
      key: '/documents',
      icon: <FileTextOutlined />,
      label: '文档中心',
    },
    {
      key: '/tasks',
      icon: <CheckSquareOutlined />,
      label: '我的任务',
    },
    ...(user?.roles?.includes('ADMIN')
      ? [
          {
            key: '/admin',
            icon: <SettingOutlined />,
            label: '系统管理',
            children: [
              {
                key: '/admin/users',
                label: '用户管理',
              },
              {
                key: '/admin/roles',
                label: '角色权限',
              },
            ],
          },
        ]
      : []),
  ];

  const userMenuItems = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: '个人中心',
    },
    {
      type: 'divider' as const,
      key: 'divider',
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      onClick: handleLogout,
    },
  ];

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', background: '#001529' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
          <Title level={4} style={{ color: '#fff', margin: 0 }}>
            多人协作编辑
          </Title>
        </div>

        <Space size="middle">
          <NotificationBell />
          <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
            <Space style={{ cursor: 'pointer' }}>
              <Avatar icon={<UserOutlined />} />
              <span style={{ color: '#fff' }}>{user?.username}</span>
            </Space>
          </Dropdown>
        </Space>
      </Header>

      <Layout>
        <Sider width={200} style={{ background: '#fff' }}>
          <Menu
            mode="inline"
            selectedKeys={[pathname]}
            defaultOpenKeys={['/admin']}
            style={{ height: '100%', borderRight: 0 }}
            items={menuItems}
            onClick={({ key }) => router.push(key)}
          />
        </Sider>
        <Layout style={{ padding: '24px' }}>
          <Content
            style={{
              background: '#fff',
              padding: 0,
              margin: 0,
              minHeight: 280,
            }}
          >
            {children}
          </Content>
        </Layout>
      </Layout>
    </Layout>
  );
}
