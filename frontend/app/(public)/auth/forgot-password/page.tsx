"use client";

import { Form, Input, Button, message } from "antd";
import { useState } from "react";
import AuthLayout from "@/components/auth/AuthLayout";
import { authService } from "@/services/auth";

const ForgotPasswordPage = () => {
  const [loading, setLoading] = useState(false);

  const handleFinish = async (values: { email: string }) => {
    try {
      setLoading(true);
      await authService.forgotPassword(values);
      message.success("密码重置邮件已发送");
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout title="找回密码" description="输入注册邮箱接收重置链接">
      <Form layout="vertical" onFinish={handleFinish}>
        <Form.Item label="注册邮箱" name="email" rules={[{ required: true }, { type: "email", message: "邮箱格式不正确" }]}>
          <Input placeholder="name@example.com" />
        </Form.Item>
        <Button type="primary" htmlType="submit" block loading={loading}>
          发送重置邮件
        </Button>
      </Form>
    </AuthLayout>
  );
};

export default ForgotPasswordPage;
