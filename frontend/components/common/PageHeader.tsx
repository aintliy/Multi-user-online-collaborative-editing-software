"use client";

import { Breadcrumb, Space, Typography } from "antd";

interface PageHeaderProps {
  title: string;
  description?: string;
  breadcrumb?: { label: string; href?: string }[];
  extra?: React.ReactNode;
}

const PageHeader = ({ title, description, breadcrumb, extra }: PageHeaderProps) => {
  return (
    <div className="page-section" style={{ marginBottom: 24 }}>
      <Space direction="vertical" style={{ width: "100%" }} size={12}>
        {breadcrumb && (
          <Breadcrumb
            items={breadcrumb.map((item) => ({
              title: item.href ? <a href={item.href}>{item.label}</a> : item.label,
            }))}
          />
        )}
        <Space align="baseline" style={{ justifyContent: "space-between" }}>
          <div>
            <Typography.Title level={3} style={{ margin: 0 }}>
              {title}
            </Typography.Title>
            {description && (
              <Typography.Text type="secondary">{description}</Typography.Text>
            )}
          </div>
          {extra}
        </Space>
      </Space>
    </div>
  );
};

export default PageHeader;
