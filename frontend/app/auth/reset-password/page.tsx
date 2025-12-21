'use client';

import React, { useState, useEffect } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { Form, Input, Button, message, Result } from 'antd';
import { LockOutlined } from '@ant-design/icons';
import { resetPassword } from '@/lib/api/auth';

export default function ResetPasswordPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const [token, setToken] = useState<string | null>(null);

  useEffect(() => {
    const tokenParam = searchParams.get('token');
    if (!tokenParam) {
      message.error('无效的重置链接');
      router.push('/login');
    } else {
      setToken(tokenParam);
    }
  }, [searchParams, router]);

  const onFinish = async (values: { password: string; confirmPassword: string }) => {
    if (!token) {
      message.error('无效的重置链接');
      return;
    }

    if (values.password !== values.confirmPassword) {
      message.error('两次输入的密码不一致');
      return;
    }

    setLoading(true);
    try {
      await resetPassword({
        token,
        newPassword: values.password,
      });
      message.success('密码重置成功！');
      setSuccess(true);
      // 3秒后跳转到登录页
      setTimeout(() => {
        router.push('/login');
      }, 3000);
    } catch (error: any) {
      if (error.message?.includes('过期') || error.message?.includes('无效')) {
        message.error('重置链接已过期或无效，请重新申请');
      } else {
        message.error(error.message || '密码重置失败');
      }
    } finally {
      setLoading(false);
    }
  };

  if (success) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100">
        <div className="bg-white p-8 rounded-lg shadow-lg w-full max-w-md">
          <Result
            status="success"
            title="密码重置成功"
            subTitle="您的密码已成功重置，即将跳转到登录页面..."
            extra={[
              <Button type="primary" key="login" onClick={() => router.push('/login')}>
                立即登录
              </Button>,
            ]}
          />
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100">
      <div className="bg-white p-8 rounded-lg shadow-lg w-full max-w-md">
        <h1 className="text-3xl font-bold text-center mb-6 text-gray-800">
          重置密码
        </h1>
        <p className="text-center text-gray-600 mb-8">
          请输入您的新密码
        </p>

        <Form
          form={form}
          name="reset-password"
          onFinish={onFinish}
          layout="vertical"
          size="large"
        >
          <Form.Item
            name="password"
            rules={[
              { required: true, message: '请输入新密码' },
              { min: 6, message: '密码至少需要6个字符' },
            ]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="新密码（至少6位）"
            />
          </Form.Item>

          <Form.Item
            name="confirmPassword"
            dependencies={['password']}
            rules={[
              { required: true, message: '请确认新密码' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('password') === value) {
                    return Promise.resolve();
                  }
                  return Promise.reject(new Error('两次输入的密码不一致'));
                },
              }),
            ]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="确认新密码"
            />
          </Form.Item>

          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              className="w-full"
              loading={loading}
            >
              重置密码
            </Button>
          </Form.Item>

          <div className="text-center text-sm text-gray-500">
            <p>重置链接有效期为30分钟</p>
          </div>
        </Form>
      </div>
    </div>
  );
}
