import React, { useEffect, useState, useCallback } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import {
  Layout,
  Menu,
  Avatar,
  Dropdown,
  Badge,
  Space,
  Button,
  message,
  notification,
  Tooltip,
} from 'antd';
import {
  FileTextOutlined,
  UserOutlined,
  TeamOutlined,
  BellOutlined,
  LogoutOutlined,
  SettingOutlined,
  GlobalOutlined,
  MessageOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import { useAuthStore } from '../store/useAuthStore';
import { notificationApi } from '../api';
import wsService from '../utils/websocket';
import heartbeatService from '../utils/heartbeat';
import { getAvatarUrl } from '../utils/request';
import './MainLayout.scss';

const { Header, Sider, Content } = Layout;

const MainLayout: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { user, token, logout } = useAuthStore();
  const [collapsed, setCollapsed] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);
  const [pendingInvitesCount, setPendingInvitesCount] = useState(0);
  const [unreadMessageMap, setUnreadMessageMap] = useState<Map<number, number>>(new Map()); // 按好友ID追踪未读数

  useEffect(() => {
    // 连接 WebSocket
    if (token) {
      wsService.connect(token).then(() => {
        wsService.subscribeToNotifications(user!.id, (data: any) => {
          // 处理好友消息通知
          if (data.type === 'FRIEND_MESSAGE') {
            handleFriendMessageNotification(data);
          } else {
            // 其他通知刷新未读数
            fetchUnreadCount();
            // 触发心跳刷新
            heartbeatService.refresh();
          }
        });
      }).catch((err) => {
        console.error('WebSocket connection failed:', err);
      });
      
      // 启动心跳服务
      heartbeatService.start();
      
      // 订阅心跳数据
      const unsubscribe = heartbeatService.subscribe((data) => {
        setUnreadCount(data.unreadNotificationCount);
        setPendingInvitesCount(data.pendingInvitesCount);
      });
      
      return () => {
        wsService.disconnect();
        heartbeatService.stop();
        unsubscribe();
      };
    }

    return () => {
      wsService.disconnect();
    };
  }, [token]);

  // 打开好友聊天
  const openFriendChat = useCallback((senderId: number, senderName: string, senderAvatar: string) => {
    // 清除该好友的未读消息
    setUnreadMessageMap(prev => {
      const newMap = new Map(prev);
      newMap.delete(senderId);
      return newMap;
    });
    
    // 导航到好友页面并触发打开聊天事件
    navigate('/friends');
    setTimeout(() => {
      window.dispatchEvent(new CustomEvent('open-friend-chat', { 
        detail: { 
          friendId: senderId, 
          friendName: senderName, 
          friendAvatar: senderAvatar 
        } 
      }));
    }, 100);
    notification.destroy();
  }, [navigate]);

  // 处理好友消息实时通知
  const handleFriendMessageNotification = useCallback((data: any) => {
    const senderId = data.senderId;
    
    // 更新该好友的未读消息数
    setUnreadMessageMap(prev => {
      const newMap = new Map(prev);
      newMap.set(senderId, (newMap.get(senderId) || 0) + 1);
      return newMap;
    });
    
    const messageContent = data.message?.messageType === 'SHARE_LINK' 
      ? '分享了一个文档给你' 
      : data.message?.content?.substring(0, 50) || '发来一条消息';
    
    const key = `friend-msg-${senderId}-${Date.now()}`;
    
    notification.info({
      key,
      title: `来自 ${data.senderName} 的消息`,
      description: (
        <div>
          <div style={{ marginBottom: 8 }}>{messageContent}</div>
          <Button 
            type="primary" 
            size="small"
            icon={<MessageOutlined />}
            onClick={() => openFriendChat(senderId, data.senderName, data.senderAvatar)}
          >
            打开聊天
          </Button>
        </div>
      ),
      icon: <Avatar size="small" src={getAvatarUrl(data.senderAvatar)} icon={<UserOutlined />} />,
      placement: 'topRight',
      duration: 6,
    });

    // 触发事件，让 Friends 页面可以刷新消息
    window.dispatchEvent(new CustomEvent('friend-message-received', { detail: data }));
  }, [openFriendChat]);

  useEffect(() => {
    fetchUnreadCount();

    const handleLocalRead = () => fetchUnreadCount();
    window.addEventListener('notification-read', handleLocalRead);

    // 监听好友消息已查看事件（传入friendId清除特定好友，不传则清除全部）
    const handleFriendMessagesViewed = (event: CustomEvent) => {
      const friendId = event.detail?.friendId;
      if (friendId) {
        setUnreadMessageMap(prev => {
          const newMap = new Map(prev);
          newMap.delete(friendId);
          return newMap;
        });
      } else {
        setUnreadMessageMap(new Map());
      }
    };
    window.addEventListener('friend-messages-viewed', handleFriendMessagesViewed as EventListener);

    return () => {
      window.removeEventListener('notification-read', handleLocalRead);
      window.removeEventListener('friend-messages-viewed', handleFriendMessagesViewed as EventListener);
    };
  }, []);

  // 计算总未读消息数
  const totalUnreadMessageCount = Array.from(unreadMessageMap.values()).reduce((a, b) => a + b, 0);

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
      key: '/collaborating',
      icon: <TeamOutlined />,
      label: '协作文档',
    },
    {
      key: '/public',
      icon: <GlobalOutlined />,
      label: '公开文档',
    },
    {
      key: '/friends',
      icon: <Badge count={totalUnreadMessageCount} size="small"><MessageOutlined /></Badge>,
      label: '好友',
    },
    {
      key: '/notifications',
      icon: <Badge count={unreadCount + pendingInvitesCount} size="small"><BellOutlined /></Badge>,
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
              <Tooltip title="刷新消息">
                <Button
                  type="text"
                  icon={<ReloadOutlined />}
                  onClick={() => {
                    heartbeatService.refresh();
                    message.success('已刷新');
                  }}
                />
              </Tooltip>
              <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
                <Space className="user-info" style={{ cursor: 'pointer' }}>
                  <Avatar 
                    src={getAvatarUrl(user?.avatarUrl)} 
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
