"use client";

import { Form, Input, Button, Space } from "antd";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";
import AuthLayout from "@/components/auth/AuthLayout";
import { useAuth } from "@/hooks/useAuth";

interface LoginFormValues {
  email: string;
  password: string;
}

const LoginForm = () => {
  const { login } = useAuth();
  const router = useRouter();
  const searchParams = useSearchParams();
  const [loading, setLoading] = useState(false);

  const handleFinish = async (values: LoginFormValues) => {
    try {
      setLoading(true);
      const loggedInUser = await login(values);
      const redirect = searchParams.get("redirect");
      if (loggedInUser.role === "ADMIN") {
        router.replace("/admin/users");
      } else {
        router.replace(redirect || "/documents");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout title="欢迎回来" description="登录多人在线协作编辑平台">
      <Form layout="vertical" onFinish={handleFinish} requiredMark={false}>
        <Form.Item label="邮箱" name="email" rules={[{ required: true, message: "请输入邮箱" }, { type: "email", message: "邮箱格式不正确" }]}>
          <Input placeholder="name@example.com" autoComplete="email" />
        </Form.Item>
        <Form.Item label="密码" name="password" rules={[{ required: true, message: "请输入密码" }]}>
          <Input.Password placeholder="请输入密码" autoComplete="current-password" />
        </Form.Item>
        <Button type="primary" htmlType="submit" block size="large" loading={loading}>
          登录
        </Button>
        <Space style={{ marginTop: 16, width: "100%", justifyContent: "space-between" }}>
          <Link href="/auth/forgot-password">忘记密码？</Link>
          <Link href="/auth/register">还没有账号？立即注册</Link>
        </Space>
      </Form>
    </AuthLayout>
  );
};

export default LoginForm;
