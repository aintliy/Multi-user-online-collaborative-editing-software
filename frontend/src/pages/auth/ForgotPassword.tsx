import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { Form, Input, Button, message, Result } from 'antd';
import { MailOutlined } from '@ant-design/icons';
import { authApi } from '../../api';

const ForgotPassword: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [sent, setSent] = useState(false);

  const handleSubmit = async (values: { email: string }) => {
    setLoading(true);
    try {
      await authApi.forgotPassword(values.email);
      setSent(true);
    } catch (error: any) {
      message.error(error.response?.data?.message || '发送失败');
    } finally {
      setLoading(false);
    }
  };

  if (sent) {
    return (
      <Result
        status="success"
        title="邮件已发送"
        subTitle="重置密码链接已发送到您的邮箱，请查收。"
        extra={
          <Link to="/login">
            <Button type="primary">返回登录</Button>
          </Link>
        }
      />
    );
  }

  return (
    <>
      <h2 className="auth-title">忘记密码</h2>
      <p style={{ textAlign: 'center', color: '#666', marginBottom: 24 }}>
        请输入您的注册邮箱，我们将发送重置密码链接。
      </p>
      <Form
        name="forgot-password"
        className="auth-form"
        onFinish={handleSubmit}
        autoComplete="off"
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
            placeholder="邮箱"
            size="large"
          />
        </Form.Item>

        <Form.Item>
          <Button
            type="primary"
            htmlType="submit"
            className="submit-btn"
            loading={loading}
          >
            发送重置链接
          </Button>
        </Form.Item>
      </Form>

      <div className="auth-links">
        <Link to="/login">返回登录</Link>
      </div>
    </>
  );
};

export default ForgotPassword;
