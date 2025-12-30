import React, { useEffect, useState } from 'react';
import { Card, Avatar, Form, Input, Button, Upload, message, Divider, Descriptions } from 'antd';
import { UserOutlined, UploadOutlined, MailOutlined, IdcardOutlined, InboxOutlined } from '@ant-design/icons';
import type { UploadProps } from 'antd';
import { useAuthStore } from '../store/useAuthStore';
import { authApi, userApi } from '../api';
import './Profile.scss';

const { Dragger } = Upload;

const Profile: React.FC = () => {
  const { user, updateUser } = useAuthStore();
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [uploadLoading, setUploadLoading] = useState(false);

  useEffect(() => {
    if (user) {
      form.setFieldsValue({
        username: user.username,
        profile: user.profile,
      });
    }
  }, [user, form]);

  const handleSubmit = async (values: any) => {
    setLoading(true);
    try {
      const updatedUser = await authApi.updateProfile(values);
      updateUser(updatedUser);
      message.success('个人资料更新成功');
    } catch (error: any) {
      message.error(error.response?.data?.message || '更新失败');
    } finally {
      setLoading(false);
    }
  };

  const uploadProps: UploadProps = {
    name: 'file',
    accept: 'image/*',
    listType: 'picture',
    maxCount: 1,
    showUploadList: true,
    customRequest: async (options) => {
      const { file, onSuccess, onError } = options;
      setUploadLoading(true);
      try {
        const result = await userApi.uploadAvatar(file as File);
        // 确保头像URL是完整的
        const fullAvatarUrl = result.avatarUrl.startsWith('http') 
          ? result.avatarUrl 
          : `http://localhost:8080${result.avatarUrl}`;
        updateUser({ ...user!, avatarUrl: fullAvatarUrl });
        message.success('头像上传成功');
        onSuccess?.(result);
      } catch (error: any) {
        message.error(error.response?.data?.message || '上传失败');
        onError?.(error);
      } finally {
        setUploadLoading(false);
      }
    },
    beforeUpload: (file) => {
      const isImage = file.type.startsWith('image/');
      if (!isImage) {
        message.error('只能上传图片文件');
        return false;
      }
      const isLt2M = file.size / 1024 / 1024 < 2;
      if (!isLt2M) {
        message.error('图片大小不能超过2MB');
        return false;
      }
      return true;
    },
  };

  return (
    <div className="profile-page">
      <Card title="个人中心" className="profile-card">
        <div className="avatar-section">
          <Avatar
            size={100}
            src={user?.avatarUrl ? (user.avatarUrl.startsWith('http') ? user.avatarUrl : `http://localhost:8080${user.avatarUrl}`) : undefined}
            icon={<UserOutlined />}
          />
          <Dragger {...uploadProps} style={{ marginTop: 16, maxWidth: 400 }}>
            <p className="ant-upload-drag-icon">
              <InboxOutlined />
            </p>
            <p className="ant-upload-text">点击或拖拽图片到此区域上传</p>
            <p className="ant-upload-hint">支持 JPG、PNG、GIF 格式，大小不超过 2MB</p>
          </Dragger>
        </div>

        <Divider />

        <Descriptions column={1} className="user-info">
          <Descriptions.Item label={<><MailOutlined /> 邮箱</>}>
            {user?.email}
          </Descriptions.Item>
          <Descriptions.Item label={<><IdcardOutlined /> 公开ID</>}>
            {user?.publicId}
          </Descriptions.Item>
          <Descriptions.Item label="角色">
            {user?.role === 'ADMIN' ? '管理员' : '普通用户'}
          </Descriptions.Item>
        </Descriptions>

        <Divider />

        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
          className="profile-form"
        >
          <Form.Item
            name="username"
            label="用户名"
            rules={[
              { required: true, message: '请输入用户名' },
              { min: 2, max: 20, message: '用户名长度为2-20个字符' },
            ]}
          >
            <Input placeholder="请输入用户名" />
          </Form.Item>

          <Form.Item
            name="profile"
            label="个人简介"
          >
            <Input.TextArea
              placeholder="介绍一下自己吧..."
              rows={4}
              maxLength={200}
              showCount
            />
          </Form.Item>

          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading}>
              保存修改
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
};

export default Profile;
