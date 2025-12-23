"use client";

import { Form, Input, Button, Result, message } from "antd";
import { useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";
import AuthLayout from "@/components/auth/AuthLayout";
import { authService } from "@/services/auth";

const ResetPasswordForm = () => {
  const searchParams = useSearchParams();
  const router = useRouter();
  const token = searchParams.get("token") ?? "";
  const [loading, setLoading] = useState(false);

  if (!token) {
    return (
      <Result
        status="error"
        title="链接无效"
        subTitle="缺少重置令牌，请重新申请邮件"
        extra={<Button onClick={() => router.push("/auth/forgot-password")}>重新发送</Button>}
      />
    );
  }

  const handleFinish = async (values: { password: string; confirm: string }) => {
    if (values.password !== values.confirm) {
      message.error("两次密码不一致");
      return;
    }
    try {
      setLoading(true);
      await authService.resetPassword({ token, newPassword: values.password });
      message.success("密码重置成功");
      router.replace("/auth/login");
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout title="重置密码">
      <Form layout="vertical" onFinish={handleFinish}>
        <Form.Item label="新密码" name="password" rules={[{ required: true, min: 6 }]}>
          <Input.Password />
        </Form.Item>
        <Form.Item
          label="确认密码"
          name="confirm"
          dependencies={["password"]}
          rules={[
            { required: true },
            ({ getFieldValue }) => ({
              validator(_, value) {
                return !value || getFieldValue("password") === value ? Promise.resolve() : Promise.reject(new Error("两次密码不一致"));
              },
            }),
          ]}
        >
          <Input.Password />
        </Form.Item>
        <Button type="primary" htmlType="submit" block loading={loading}>
          重置密码
        </Button>
      </Form>
    </AuthLayout>
  );
};

export default ResetPasswordForm;
