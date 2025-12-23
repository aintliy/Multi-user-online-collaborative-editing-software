"use client";

import { Drawer, Form, Input, Select, Button } from "antd";
import { useState } from "react";
import { CreateDocumentPayload } from "@/services/documents";

interface DocumentCreateDrawerProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (payload: CreateDocumentPayload) => Promise<void>;
}

const DocumentCreateDrawer = ({ open, onClose, onSubmit }: DocumentCreateDrawerProps) => {
  const [form] = Form.useForm<CreateDocumentPayload>();
  const [loading, setLoading] = useState(false);

  const handleFinish = async (values: CreateDocumentPayload) => {
    try {
      setLoading(true);
      await onSubmit(values);
      form.resetFields();
      onClose();
    } finally {
      setLoading(false);
    }
  };

  return (
    <Drawer title="新建文档" open={open} onClose={onClose} width={420} destroyOnClose>
      <Form layout="vertical" form={form} onFinish={handleFinish} initialValues={{ visibility: "private", type: "markdown" }}>
        <Form.Item name="title" label="文档标题" rules={[{ required: true, message: "请输入标题" }]}> 
          <Input placeholder="例如：产品设计文档" />
        </Form.Item>
        <Form.Item name="type" label="文档类型" rules={[{ required: true }]}> 
          <Select
            options={[
              { label: "Markdown", value: "markdown" },
              { label: "Docx", value: "docx" },
              { label: "Txt", value: "txt" },
            ]}
          />
        </Form.Item>
        <Form.Item name="visibility" label="可见性" rules={[{ required: true }]}> 
          <Select
            options={[
              { label: "私有", value: "private" },
              { label: "公开", value: "public" },
            ]}
          />
        </Form.Item>
        <Form.Item name="tags" label="标签">
          <Input placeholder="以逗号分隔" />
        </Form.Item>
        <Button type="primary" htmlType="submit" block loading={loading}>
          创建
        </Button>
      </Form>
    </Drawer>
  );
};

export default DocumentCreateDrawer;
