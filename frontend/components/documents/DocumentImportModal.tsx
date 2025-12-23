"use client";

import { Modal, Upload, message, Select, Form } from "antd";
import { InboxOutlined } from "@ant-design/icons";
import { useState } from "react";
import type { UploadProps } from "antd";
import type { RcFile } from "antd/es/upload/interface";
import type { DocumentVisibility } from "@/types";

interface DocumentImportModalProps {
  open: boolean;
  onClose: () => void;
  onImport: (formData: FormData) => Promise<void>;
}

interface ImportFormValues {
  visibility: DocumentVisibility;
  type: "markdown" | "docx" | "txt";
}

const DocumentImportModal = ({ open, onClose, onImport }: DocumentImportModalProps) => {
  const [uploading, setUploading] = useState(false);
  const [form] = Form.useForm();

  const handleUpload: UploadProps["customRequest"] = async ({ file, onSuccess, onError }) => {
    try {
      setUploading(true);
      const formData = new FormData();
      formData.append("file", file as RcFile);
      const { visibility = "private", type = "markdown" } = form.getFieldsValue() as ImportFormValues;
      formData.append("visibility", visibility);
      formData.append("type", type);
      await onImport(formData);
      message.success("导入成功");
      onClose();
      onSuccess?.("ok");
    } catch (error) {
      onError?.(error as Error);
    } finally {
      setUploading(false);
    }
  };

  return (
    <Modal title="导入文档" open={open} onCancel={onClose} onOk={() => form.submit()} okButtonProps={{ loading: uploading }}>
      <Form form={form} layout="vertical" initialValues={{ visibility: "private", type: "markdown" }}>
        <Form.Item label="文档可见性" name="visibility">
          <Select
            options={[
              { label: "私有", value: "private" },
              { label: "公开", value: "public" },
            ]}
          />
        </Form.Item>
        <Form.Item label="文件类型" name="type">
          <Select
            options={[
              { label: "Markdown", value: "markdown" },
              { label: "Docx", value: "docx" },
              { label: "Txt", value: "txt" },
            ]}
          />
        </Form.Item>
      </Form>
      <Upload.Dragger name="file" multiple={false} customRequest={handleUpload} accept=".md,.markdown,.txt,.docx" showUploadList={false} disabled={uploading}>
        <p className="ant-upload-drag-icon">
          <InboxOutlined />
        </p>
        <p className="ant-upload-text">点击或拖拽文件到此上传</p>
        <p className="ant-upload-hint">支持 Markdown / Docx / Txt 文档，大小不超过 5MB</p>
      </Upload.Dragger>
    </Modal>
  );
};

export default DocumentImportModal;
