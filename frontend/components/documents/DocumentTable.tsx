"use client";

import { Table, Tag, Space, Button, Tooltip } from "antd";
import type { ColumnsType } from "antd/es/table";
import dayjs from "dayjs";
import { Document } from "@/types";

interface DocumentTableProps {
  data: Document[];
  loading: boolean;
  total: number;
  page: number;
  pageSize: number;
  onPageChange: (page: number, pageSize: number) => void;
  onView: (record: Document) => void;
  onDelete: (record: Document) => void;
  onClone: (record: Document) => void;
}

const DocumentTable = ({ data, loading, total, page, pageSize, onPageChange, onView, onDelete, onClone }: DocumentTableProps) => {
  const columns: ColumnsType<Document> = [
    {
      title: "文档标题",
      dataIndex: "title",
      render: (text, record) => (
        <Space direction="vertical" size={0}>
          <a onClick={() => onView(record)} style={{ fontWeight: 600 }}>
            {text}
          </a>
          <span style={{ color: "var(--text-muted)", fontSize: 12 }}>{record.ownerName}</span>
        </Space>
      ),
    },
    {
      title: "类型",
      dataIndex: "docType",
      width: 120,
      render: (value) => value?.toUpperCase(),
    },
    {
      title: "可见性",
      dataIndex: "visibility",
      width: 120,
      render: (visibility) => (
        <Tag color={visibility === "public" ? "green" : "gold"}>
          {visibility === "public" ? "公开" : "私有"}
        </Tag>
      ),
    },
    {
      title: "最近更新",
      dataIndex: "updatedAt",
      width: 200,
      render: (value) => dayjs(value).format("YYYY-MM-DD HH:mm"),
    },
    {
      title: "操作",
      key: "actions",
      width: 220,
      render: (_, record) => (
        <Space>
          <Button type="link" onClick={() => onView(record)}>
            打开
          </Button>
          <Tooltip title="克隆一份属于自己的副本">
            <Button type="link" onClick={() => onClone(record)}>
              克隆
            </Button>
          </Tooltip>
          {record.isOwner && (
            <Button danger type="link" onClick={() => onDelete(record)}>
              删除
            </Button>
          )}
        </Space>
      ),
    },
  ];

  return (
    <Table
      rowKey="id"
      columns={columns}
      dataSource={data}
      loading={loading}
      pagination={{
        current: page,
        pageSize,
        total,
        onChange: onPageChange,
        showSizeChanger: true,
        showTotal: (t) => `共 ${t} 条文档`,
      }}
    />
  );
};

export default DocumentTable;
