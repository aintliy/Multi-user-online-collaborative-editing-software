import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Form, Input, Button, message } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import { authApi } from '../../api';
import { useAuthStore } from '../../store/useAuthStore';

interface LoginFormData {
  email: string;
  password: string;
}

const Login: React.FC = () => {
  const navigate = useNavigate();
  const { setAuth } = useAuthStore();
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (values: LoginFormData) => {
    setLoading(true);
    try {
      const response = await authApi.login(values);
      setAuth(response.token, response.user);
      message.success('登录成功');
      const targetPath = response.user.role === 'ADMIN' ? '/admin' : '/';
      navigate(targetPath);
    } catch (error: any) {
      message.error(error.response?.data?.message || '登录失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <h2 className="auth-title">登录</h2>
      <Form
        name="login"
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
            prefix={<UserOutlined />}
            placeholder="邮箱"
            size="large"
          />
        </Form.Item>

        <Form.Item
          name="password"
          rules={[{ required: true, message: '请输入密码' }]}
        >
          <Input.Password
            prefix={<LockOutlined />}
            placeholder="密码"
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
            登录
          </Button>
        </Form.Item>
      </Form>

      <div className="auth-links">
        <Link to="/forgot-password">忘记密码？</Link>
        <span style={{ margin: '0 8px' }}>|</span>
        <Link to="/register">注册新账号</Link>
      </div>
    </>
  );
};

export default Login;
