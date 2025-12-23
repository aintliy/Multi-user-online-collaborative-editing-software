"use client";

import { Layout, Menu } from "antd";
import { FileTextOutlined, TeamOutlined, RocketOutlined, UserOutlined, UnorderedListOutlined, CrownOutlined } from "@ant-design/icons";
import { usePathname, useRouter } from "next/navigation";
import { useAuth } from "@/hooks/useAuth";
import { useMemo } from "react";

const AppSider = ({ collapsed }: { collapsed: boolean }) => {
  const router = useRouter();
  const pathname = usePathname();
  const { user } = useAuth();

  const items = useMemo(() => {
    const base = [
      { key: "/documents", icon: <FileTextOutlined />, label: "文档中心" },
      { key: "/tasks", icon: <UnorderedListOutlined />, label: "我的任务" },
      { key: "/friends", icon: <TeamOutlined />, label: "好友与协作" },
      { key: "/explore", icon: <RocketOutlined />, label: "发现" },
      { key: "/profile", icon: <UserOutlined />, label: "个人中心" },
    ];
    if (user?.role === "ADMIN") {
      base.push({ key: "/admin/users", icon: <CrownOutlined />, label: "系统管理" });
    }
    return base;
  }, [user]);

  return (
    <Layout.Sider collapsible collapsed={collapsed} trigger={null} width={220} style={{ background: "#050816", color: "#fff" }}>
      <div style={{ padding: collapsed ? "16px 8px" : "24px", color: "#fff", fontWeight: 600, fontSize: collapsed ? 16 : 20 }}>
        协作空间
      </div>
      <Menu
        theme="dark"
        mode="inline"
        selectedKeys={[pathname?.startsWith("/admin") ? "/admin/users" : pathname || "/documents"]}
        items={items}
        onClick={({ key }) => router.push(key)}
        style={{ background: "transparent", borderInlineEnd: "none" }}
      />
    </Layout.Sider>
  );
};

export default AppSider;
