"use client";

import { Layout } from "antd";
import { useState } from "react";
import AppHeader from "@/components/layout/AppHeader";
import AppSider from "@/components/layout/AppSider";

const MainLayout = ({ children }: { children: React.ReactNode }) => {
  const [collapsed, setCollapsed] = useState(false);
  return (
    <Layout>
      <AppSider collapsed={collapsed} />
      <Layout style={{ background: "transparent" }}>
        <AppHeader collapsed={collapsed} onToggle={() => setCollapsed((prev) => !prev)} />
        <Layout.Content style={{ padding: "24px", minHeight: "calc(100vh - 64px)", background: "#f5f7fb" }}>
          <div style={{ maxWidth: 1280, margin: "0 auto" }}>{children}</div>
        </Layout.Content>
      </Layout>
    </Layout>
  );
};

export default MainLayout;
