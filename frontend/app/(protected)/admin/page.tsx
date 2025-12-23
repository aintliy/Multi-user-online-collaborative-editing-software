"use client";

import { useEffect, useState } from "react";
import { Row, Col } from "antd";
import PageHeader from "@/components/common/PageHeader";
import StatCard from "@/components/common/StatCard";
import { adminService, SystemStats } from "@/services/admin";

const AdminDashboardPage = () => {
  const [stats, setStats] = useState<SystemStats | null>(null);

  useEffect(() => {
    void (async () => {
      try {
        const data = await adminService.getStats();
        setStats(data);
      } catch {
        setStats(null);
      }
    })();
  }, []);

  return (
    <div>
      <PageHeader title="系统概览" description="管理员仪表盘" />
      <Row gutter={16}>
        <Col span={6}><StatCard label="用户数" value={stats?.userCount ?? "--"} /></Col>
        <Col span={6}><StatCard label="文档数" value={stats?.documentCount ?? "--"} accent="#f97316" /></Col>
        <Col span={6}><StatCard label="任务数" value={stats?.taskCount ?? "--"} accent="#22c55e" /></Col>
        <Col span={6}><StatCard label="实时协作" value={stats?.activeCollaborationSessions ?? "--"} accent="#6366f1" /></Col>
      </Row>
    </div>
  );
};

export default AdminDashboardPage;
