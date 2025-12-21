'use client';

import React, { useState } from 'react';
import { useRouter } from 'next/navigation';
import { Form, Input, Button, message, Result } from 'antd';
import { MailOutlined, ArrowLeftOutlined } from '@ant-design/icons';
import { forgotPassword } from '@/lib/api/auth';
import Link from 'next/link';

export default function ForgotPasswordPage() {
  const router = useRouter();
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [countdown, setCountdown] = useState(0);
  const [emailSent, setEmailSent] = useState(false);

  React.useEffect(() => {
    if (countdown > 0) {
      const timer = setTimeout(() => setCountdown(countdown - 1), 1000);
      return () => clearTimeout(timer);
    }
  }, [countdown]);

  const onFinish = async (values: { email: string }) => {
    setLoading(true);
    try {
      await forgotPassword(values.email);
      message.success('重置密码邮件已发送，请查收邮箱');
      setEmailSent(true);
      setCountdown(60); // 60秒倒计时
    } catch (error: any) {
      if (error.message?.includes('频率限制') || error.message?.includes('TOO_MANY_REQUESTS')) {
        message.error('请求过于频繁，请稍后再试');
        setCountdown(60);
      } else {
        message.error(error.message || '发送失败，请稍后重试');
      }
    } finally {
      setLoading(false);
    }
  };

  if (emailSent) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100">
        <div className="bg-white p-8 rounded-lg shadow-lg w-full max-w-md">
          <Result
            status="success"
            title="邮件已发送"
            subTitle="请查收您的邮箱，点击邮件中的链接重置密码。链接有效期为30分钟。"
            extra={[
              <Button type="primary" key="login" onClick={() => router.push('/login')}>
                返回登录
              </Button>,
              <Button key="resend" disabled={countdown > 0} onClick={() => setEmailSent(false)}>
                {countdown > 0 ? `重新发送 (${countdown}s)` : '重新发送'}
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
        <div className="mb-6">
          <Link href="/login" className="text-blue-600 hover:text-blue-700 inline-flex items-center">
            <ArrowLeftOutlined className="mr-2" />
            返回登录
          </Link>
        </div>

        <h1 className="text-3xl font-bold text-center mb-6 text-gray-800">
          忘记密码
        </h1>
        <p className="text-center text-gray-600 mb-8">
          输入您的邮箱地址，我们将发送重置密码链接
        </p>

        <Form
          form={form}
          name="forgot-password"
          onFinish={onFinish}
          layout="vertical"
          size="large"
        >
          <Form.Item
            name="email"
            rules={[
              { required: true, message: '请输入邮箱' },
              { type: 'email', message: '请输入有效的邮箱地址' },
            ]}
          >
            <Input
              prefix={<MailOutlined />}
              placeholder="注册时使用的邮箱"
              disabled={countdown > 0}
            />
          </Form.Item>

          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              className="w-full"
              loading={loading}
              disabled={countdown > 0}
            >
              {countdown > 0 ? `请稍后 (${countdown}s)` : '发送重置链接'}
            </Button>
          </Form.Item>

          <div className="text-center text-sm text-gray-500">
            <p>为防止滥用，发送邮件有频率限制</p>
            <p>如未收到邮件，请检查垃圾邮件箱</p>
          </div>
        </Form>
      </div>
    </div>
  );
}
