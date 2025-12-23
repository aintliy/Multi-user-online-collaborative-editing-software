"use client";

import { Table, Space, Select, Button } from "antd";
import type { ColumnsType } from "antd/es/table";
import { User } from "@/types";

interface UserManagementTableProps {
  data: User[];
  loading: boolean;
  total: number;
  page: number;
  pageSize: number;
  onPageChange: (page: number, pageSize: number) => void;
  onRoleChange: (userId: number, role: string) => void;
  onStatusChange: (userId: number, status: string) => void;
  onResetPassword: (userId: number) => void;
}

const UserManagementTable = ({ data, loading, total, page, pageSize, onPageChange, onRoleChange, onStatusChange, onResetPassword }: UserManagementTableProps) => {
  const columns: ColumnsType<User> = [
    { title: "用户名", dataIndex: "username" },
    { title: "邮箱", dataIndex: "email" },
    { title: "Public ID", dataIndex: "publicId" },
    {
      title: "角色",
      dataIndex: "role",
      render: (value, record) => (
        <Select value={value} options={[{ value: "USER", label: "用户" }, { value: "ADMIN", label: "管理员" }]} onChange={(role) => onRoleChange(record.id, role)} />
      ),
    },
    {
      title: "状态",
      dataIndex: "status",
      render: (value, record) => (
        <Select value={value} options={[{ value: "active", label: "正常" }, { value: "disabled", label: "禁用" }]} onChange={(status) => onStatusChange(record.id, status)} />
      ),
    },
    {
      title: "操作",
      key: "actions",
      render: (_, record) => (
        <Space>
          <Button size="small" onClick={() => onResetPassword(record.id)}>
            重置密码
          </Button>
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
      pagination={{ current: page, pageSize, total, onChange: onPageChange }}
    />
  );
};

export default UserManagementTable;
