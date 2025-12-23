"use client";

import { Typography } from "antd";

interface AuthLayoutProps {
  title: string;
  description?: string;
  children: React.ReactNode;
}

const AuthLayout = ({ title, description, children }: AuthLayoutProps) => {
  return (
    <div className="auth-hero">
      <div className="glass-panel" style={{ width: "min(420px, 100%)", padding: 32 }}>
        <Typography.Title level={2} style={{ marginTop: 0 }}>
          {title}
        </Typography.Title>
        {description && (
          <Typography.Paragraph type="secondary" style={{ marginBottom: 24 }}>
            {description}
          </Typography.Paragraph>
        )}
        {children}
      </div>
    </div>
  );
};

export default AuthLayout;
