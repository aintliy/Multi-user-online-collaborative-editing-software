'use client';

import React, { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { Form, Input, Button, message, Row, Col } from 'antd';
import { UserOutlined, LockOutlined, MailOutlined, SafetyCertificateOutlined } from '@ant-design/icons';
import { register, sendVerificationCode, RegisterRequest } from '@/lib/api/auth';
import Link from 'next/link';

export default function RegisterPage() {
  const router = useRouter();
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [sendingCode, setSendingCode] = useState(false);
  const [countdown, setCountdown] = useState(0);

  useEffect(() => {
    let timer: NodeJS.Timeout;
    if (countdown > 0) {
      timer = setInterval(() => {
        setCountdown((prev) => prev - 1);
      }, 1000);
    }
    return () => clearInterval(timer);
  }, [countdown]);

  const handleSendCode = async () => {
    try {
      const email = form.getFieldValue('email');
      if (!email) {
        message.error('请先输入邮箱');
        return;
      }
      // 简单的邮箱格式校验
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailRegex.test(email)) {
        message.error('请输入有效的邮箱地址');
        return;
      }

      setSendingCode(true);
      await sendVerificationCode(email);
      message.success('验证码已发送，请查收邮件');
      setCountdown(60); // 60秒倒计时
    } catch (error: any) {
      message.error(error.message || '发送验证码失败');
    } finally {
      setSendingCode(false);
    }
  };

  const onFinish = async (values: RegisterRequest) => {
    setLoading(true);
    try {
      await register(values);
      message.success('注册成功！请登录');
      router.push('/login');
    } catch (error: any) {
      message.error(error.message || '注册失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100">
      <div className="bg-white p-8 rounded-lg shadow-lg w-full max-w-md">
        <h1 className="text-3xl font-bold text-center mb-6 text-gray-800">
          协作编辑系统
        </h1>
        <h2 className="text-xl text-center mb-8 text-gray-600">用户注册</h2>

        <Form
          form={form}
          name="register"
          onFinish={onFinish}
          layout="vertical"
          size="large"
        >
          <Form.Item
            name="username"
            rules={[
              { required: true, message: '请输入用户名' },
              { min: 2, max: 50, message: '用户名长度为 2-50 字符' },
            ]}
          >
            <Input
              prefix={<UserOutlined />}
              placeholder="用户名"
            />
          </Form.Item>

          <Form.Item
            name="email"
            rules={[
              { required: true, message: '请输入邮箱' },
              { type: 'email', message: '请输入有效的邮箱地址' },
            ]}
          >
            <Input
              prefix={<MailOutlined />}
              placeholder="邮箱"
            />
          </Form.Item>

          <Form.Item
            name="verificationCode"
            rules={[{ required: true, message: '请输入验证码' }]}
          >
            <Row gutter={8}>
              <Col span={16}>
                <Input
                  prefix={<SafetyCertificateOutlined />}
                  placeholder="验证码"
                />
              </Col>
              <Col span={8}>
                <Button
                  onClick={handleSendCode}
                  disabled={countdown > 0 || sendingCode}
                  loading={sendingCode}
                  block
                >
                  {countdown > 0 ? `${countdown}s` : '发送验证码'}
                </Button>
              </Col>
            </Row>
          </Form.Item>

          <Form.Item
            name="password"
            rules={[
              { required: true, message: '请输入密码' },
              { min: 6, max: 20, message: '密码长度为 6-20 字符' },
            ]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="密码"
            />
          </Form.Item>

          <Form.Item
            name="confirmPassword"
            dependencies={['password']}
            rules={[
              { required: true, message: '请确认密码' },
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
              placeholder="确认密码"
            />
          </Form.Item>

          <Form.Item
            name="phone"
          >
            <Input
              placeholder="手机号（可选）"
            />
          </Form.Item>

          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              className="w-full"
              loading={loading}
            >
              注册
            </Button>
          </Form.Item>

          <div className="text-center">
            <span className="text-gray-600">已有账号？</span>
            <Link href="/login" className="text-blue-600 hover:text-blue-700 ml-1">
              立即登录
            </Link>
          </div>
        </Form>
      </div>
    </div>
  );
}
