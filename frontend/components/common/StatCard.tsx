"use client";

import { Card, Typography } from "antd";

interface StatCardProps {
  label: string;
  value: string | number;
  subTitle?: string;
  accent?: string;
}

const StatCard = ({ label, value, subTitle, accent = "#155eef" }: StatCardProps) => {
  return (
    <Card bordered={false} style={{ borderRadius: 16 }}>
      <Typography.Text type="secondary">{label}</Typography.Text>
      <Typography.Title level={3} style={{ marginTop: 8, color: accent }}>
        {value}
      </Typography.Title>
      {subTitle && (
        <Typography.Text type="secondary" style={{ fontSize: 12 }}>
          {subTitle}
        </Typography.Text>
      )}
    </Card>
  );
};

export default StatCard;
