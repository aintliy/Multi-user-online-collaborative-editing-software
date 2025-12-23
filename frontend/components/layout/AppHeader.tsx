"use client";

import { Layout, Input, Space, Badge, Avatar, Dropdown, Button, Tooltip } from "antd";
import { BellOutlined, MenuFoldOutlined, MenuUnfoldOutlined, LogoutOutlined, UserOutlined } from "@ant-design/icons";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { notificationsService } from "@/services/notifications";
import { useAuth } from "@/hooks/useAuth";

interface AppHeaderProps {
  collapsed: boolean;
  onToggle: () => void;
}

const AppHeader = ({ collapsed, onToggle }: AppHeaderProps) => {
  const router = useRouter();
  const { user, logout } = useAuth();
  const [unread, setUnread] = useState(0);

  useEffect(() => {
    const fetchUnread = async () => {
      try {
        const count = await notificationsService.unreadCount();
        setUnread(count || 0);
      } catch {
        /* 忽略 */
      }
    };
    fetchUnread();
    const timer = setInterval(fetchUnread, 60_000);
    return () => clearInterval(timer);
  }, []);

  return (
    <Layout.Header
      style={{
        padding: "0 24px",
        background: "rgba(255,255,255,0.9)",
        display: "flex",
        alignItems: "center",
        justifyContent: "space-between",
        backdropFilter: "blur(8px)",
      }}
    >
      <Space size={16} align="center">
        <Button type="text" icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />} onClick={onToggle} />
        <Input.Search placeholder="搜索文档 / 用户 / 标签" style={{ width: 320 }} onSearch={(value) => router.push(`/documents?keyword=${encodeURIComponent(value)}`)} allowClear />
      </Space>
      <Space size={16} align="center">
        <Tooltip title="通知中心">
          <Badge count={unread} size="small">
            <Button shape="circle" icon={<BellOutlined />} onClick={() => router.push("/profile?tab=notifications")} />
          </Badge>
        </Tooltip>
        <Dropdown
          menu={{
            items: [
              { key: "profile", icon: <UserOutlined />, label: "个人中心", onClick: () => router.push("/profile") },
              { key: "logout", icon: <LogoutOutlined />, label: "退出登录", onClick: logout },
            ],
          }}
        >
          <Avatar src={user?.avatarUrl} style={{ cursor: "pointer", backgroundColor: "#155eef" }}>
            {user?.username?.[0] || "U"}
          </Avatar>
        </Dropdown>
      </Space>
    </Layout.Header>
  );
};

export default AppHeader;
