'use client';

import React, { useState, useEffect } from 'react';
import { Form, Input, Button, Upload, message, Card, Avatar, Spin } from 'antd';
import { UserOutlined, PhoneOutlined, MailOutlined, UploadOutlined } from '@ant-design/icons';
import { useAuth } from '@/contexts/AuthContext';
import { uploadAvatar, updateUserProfile } from '@/lib/api/user';
import { getCurrentUser } from '@/lib/api/auth';
import type { UploadFile } from 'antd/es/upload/interface';

const { TextArea } = Input;

export default function ProfilePage() {
  const { user } = useAuth();
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [avatarLoading, setAvatarLoading] = useState(false);
  const [avatarUrl, setAvatarUrl] = useState<string | undefined>(user?.avatarUrl);

  useEffect(() => {
    if (user) {
      form.setFieldsValue({
        username: user.username,
        email: user.email,
        phone: user.phone,
        profile: user.profile,
      });
      setAvatarUrl(user.avatarUrl);
    }
  }, [user, form]);

  const handleAvatarUpload = async (file: File) => {
    // 检查文件大小（限制5MB）
    const isLt5M = file.size / 1024 / 1024 < 5;
    if (!isLt5M) {
      message.error('图片大小不能超过5MB');
      return false;
    }

    // 检查文件类型
    const isImage = file.type.startsWith('image/');
    if (!isImage) {
      message.error('只能上传图片文件');
      return false;
    }

    setAvatarLoading(true);
    try {
      const response = await uploadAvatar(file);
      setAvatarUrl(response.avatarUrl);
      message.success('头像上传成功');
      
      // 刷新用户信息
      await getCurrentUser();
      window.location.reload();
    } catch (error: any) {
      message.error(error.message || '头像上传失败');
    } finally {
      setAvatarLoading(false);
    }

    return false; // 阻止自动上传
  };

  const onFinish = async (values: any) => {
    setLoading(true);
    try {
      await updateUserProfile({
        username: values.username,
        phone: values.phone,
        profile: values.profile,
      });
      message.success('资料更新成功');
      window.location.reload();
    } catch (error: any) {
      message.error(error.message || '更新失败');
    } finally {
      setLoading(false);
    }
  };

  if (!user) {
    return (
      <div className="flex justify-center items-center h-screen">
        <Spin size="large" />
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto p-6">
      <Card title="个人资料" className="shadow-md">
        <div className="flex flex-col items-center mb-8">
          <Avatar
            size={120}
            src={avatarUrl}
            icon={<UserOutlined />}
            className="mb-4"
          />
          <Upload
            showUploadList={false}
            beforeUpload={handleAvatarUpload}
            accept="image/*"
          >
            <Button icon={<UploadOutlined />} loading={avatarLoading}>
              更换头像
            </Button>
          </Upload>
          <p className="text-gray-500 text-sm mt-2">支持JPG、PNG格式，大小不超过5MB</p>
        </div>

        <Form
          form={form}
          layout="vertical"
          onFinish={onFinish}
          className="max-w-2xl mx-auto"
        >
          <Form.Item
            label="用户名"
            name="username"
            rules={[
              { required: true, message: '请输入用户名' },
              { min: 3, max: 20, message: '用户名长度为3-20个字符' },
            ]}
          >
            <Input
              prefix={<UserOutlined />}
              placeholder="用户名"
              size="large"
            />
          </Form.Item>

          <Form.Item label="邮箱" name="email">
            <Input
              prefix={<MailOutlined />}
              disabled
              size="large"
            />
          </Form.Item>

          <Form.Item
            label="手机号"
            name="phone"
            rules={[
              { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号' },
            ]}
          >
            <Input
              prefix={<PhoneOutlined />}
              placeholder="手机号（可选）"
              size="large"
            />
          </Form.Item>

          <Form.Item
            label="个人简介"
            name="profile"
            rules={[
              { max: 200, message: '个人简介不超过200字符' },
            ]}
          >
            <TextArea
              rows={4}
              placeholder="介绍一下自己吧"
              maxLength={200}
              showCount
            />
          </Form.Item>

          <Form.Item>
            <div className="flex gap-4">
              <Button type="primary" htmlType="submit" loading={loading} size="large">
                保存修改
              </Button>
              <Button onClick={() => form.resetFields()} size="large">
                重置
              </Button>
            </div>
          </Form.Item>
        </Form>

        <div className="mt-8 pt-8 border-t border-gray-200">
          <h3 className="text-lg font-semibold mb-4">账户信息</h3>
          <div className="space-y-2 text-gray-600">
            <p>账户状态: <span className="text-green-600 font-medium">{user.status === 'ACTIVE' ? '正常' : '已禁用'}</span></p>
            <p>注册时间: {new Date(user.createdAt).toLocaleString('zh-CN')}</p>
            <p>最后更新: {new Date(user.updatedAt).toLocaleString('zh-CN')}</p>
            {user.roles && user.roles.length > 0 && (
              <p>角色: <span className="font-medium">{user.roles.join(', ')}</span></p>
            )}
          </div>
        </div>
      </Card>
    </div>
  );
}
