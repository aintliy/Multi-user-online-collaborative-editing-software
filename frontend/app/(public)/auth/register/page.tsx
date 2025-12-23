"use client";

import { Form, Input, Button, Space, message } from "antd";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import AuthLayout from "@/components/auth/AuthLayout";
import CountdownButton from "@/components/common/CountdownButton";
import { authService, RegisterPayload } from "@/services/auth";

const RegisterPage = () => {
  const [form] = Form.useForm<RegisterPayload & { confirmPassword: string }>();
  const router = useRouter();
  const [loading, setLoading] = useState(false);

  const handleSendCode = async () => {
    const email = form.getFieldValue("email");
    if (!email) {
      message.warning("请先输入邮箱");
      throw new Error("missing email");
    }
    await authService.sendVerificationCode(email);
  };

  const handleFinish = async (values: RegisterPayload & { confirmPassword: string }) => {
    try {
      setLoading(true);
      await authService.register(values);
      message.success("注册成功，请登录");
      router.push("/auth/login");
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout title="创建账号" description="注册后即可体验多人实时协作">
      <Form layout="vertical" form={form} onFinish={handleFinish} requiredMark={false}>
        <Form.Item label="邮箱" name="email" rules={[{ required: true, message: "请输入邮箱" }, { type: "email" }]}> 
          <Input placeholder="name@example.com" />
        </Form.Item>
        <Form.Item label="验证码" name="verificationCode" rules={[{ required: true, message: "请输入验证码" }]}> 
          <Space.Compact style={{ width: "100%" }}>
            <Input placeholder="六位验证码" />
            <CountdownButton action={handleSendCode}>发送验证码</CountdownButton>
          </Space.Compact>
        </Form.Item>
        <Form.Item label="用户名" name="username" rules={[{ required: true, message: "请输入用户名" }]}>
          <Input />
        </Form.Item>
        <Form.Item label="密码" name="password" rules={[{ required: true, min: 6 }]}> 
          <Input.Password placeholder="至少 6 位" />
        </Form.Item>
        <Form.Item label="确认密码" name="confirmPassword" dependencies={["password"]} rules={[{ required: true }, ({ getFieldValue }) => ({ validator(_, value) { return !value || getFieldValue("password") === value ? Promise.resolve() : Promise.reject(new Error("两次输入密码不一致")); } })]}> 
          <Input.Password />
        </Form.Item>
        <Button type="primary" htmlType="submit" block size="large" loading={loading}>
          注册
        </Button>
        <Space style={{ marginTop: 16 }}>
          已有账号？<Link href="/auth/login">去登录</Link>
        </Space>
      </Form>
    </AuthLayout>
  );
};

export default RegisterPage;
