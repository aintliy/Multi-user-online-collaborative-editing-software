"use client";

import { Card, Form, Input, Button, Upload, Avatar, message } from "antd";
import { UploadOutlined } from "@ant-design/icons";
import { useEffect } from "react";
import type { UploadProps } from "antd";
import type { RcFile } from "antd/es/upload/interface";
import { useAuth } from "@/hooks/useAuth";
import { authService } from "@/services/auth";
import { usersService } from "@/services/users";

interface ProfileFormValues {
  username: string;
  phone?: string;
  profile?: string;
}

const ProfileForm = () => {
  const { user, refresh } = useAuth();
  const [form] = Form.useForm();

  useEffect(() => {
    if (user) {
      form.setFieldsValue({
        username: user.username,
        email: user.email,
        phone: user.phone,
        profile: user.profile,
        publicId: user.publicId,
      });
    }
  }, [user, form]);

  const handleSubmit = async (values: ProfileFormValues) => {
    await authService.updateProfile({
      username: values.username,
      phone: values.phone,
      profile: values.profile,
    });
    message.success("资料已更新");
    refresh();
  };

  const handleUpload: UploadProps["customRequest"] = async ({ file, onSuccess, onError }) => {
    try {
      await usersService.uploadAvatar(file as RcFile);
      message.success("头像上传成功");
      refresh();
      onSuccess?.("ok");
    } catch (error) {
      onError?.(error as Error);
    }
  };

  return (
    <Card title="个人资料" bordered={false}>
      <Form layout="vertical" form={form} onFinish={handleSubmit}>
        <Form.Item label="头像" name="avatar">
          <Upload showUploadList={false} customRequest={handleUpload} accept="image/*">
            <Button icon={<UploadOutlined />}>上传新头像</Button>
          </Upload>
          <Avatar size={64} src={user?.avatarUrl} style={{ marginTop: 12 }}>
            {user?.username?.[0]}
          </Avatar>
        </Form.Item>
        <Form.Item label="用户名" name="username" rules={[{ required: true, message: "请输入用户名" }]}>
          <Input />
        </Form.Item>
        <Form.Item label="邮箱" name="email">
          <Input disabled />
        </Form.Item>
        <Form.Item label="手机号" name="phone">
          <Input />
        </Form.Item>
        <Form.Item label="公开 ID" name="publicId">
          <Input disabled />
        </Form.Item>
        <Form.Item label="个人简介" name="profile">
          <Input.TextArea rows={4} />
        </Form.Item>
        <Button type="primary" htmlType="submit">
          保存
        </Button>
      </Form>
    </Card>
  );
};

export default ProfileForm;
