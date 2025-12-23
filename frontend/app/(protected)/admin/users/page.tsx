"use client";

import { useCallback, useEffect, useState } from "react";
import { message, Modal } from "antd";
import PageHeader from "@/components/common/PageHeader";
import UserManagementTable from "@/components/admin/UserManagementTable";
import { adminService } from "@/services/admin";
import { User } from "@/types";

const AdminUsersPage = () => {
  const [data, setData] = useState<User[]>([]);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);

  const fetchUsers = useCallback(
    async (current = page, size = pageSize) => {
      setLoading(true);
      try {
        const result = await adminService.getUsers({ page: current, pageSize: size });
        setData(result.items);
        setPage(result.page);
        setPageSize(result.pageSize);
        setTotal(result.total);
      } finally {
        setLoading(false);
      }
    },
    [page, pageSize]
  );

  useEffect(() => {
    void fetchUsers();
  }, [fetchUsers]);

  const handleResetPassword = (userId: number) => {
    Modal.confirm({
      title: "重置该用户密码?",
      content: "新密码将随机生成",
      onOk: async () => {
        await adminService.resetUserPassword(userId, "Temp#123456");
        message.success("已重置为临时密码 Temp#123456");
      },
    });
  };

  return (
    <div>
      <PageHeader title="用户管理" description="调整系统角色与账号状态" />
      <div className="page-section">
        <UserManagementTable
          data={data}
          loading={loading}
          total={total}
          page={page}
          pageSize={pageSize}
          onPageChange={fetchUsers}
          onRoleChange={(userId, role) => adminService.updateUserRole(userId, role).then(() => {
            message.success("角色已更新");
            fetchUsers();
          })}
          onStatusChange={(userId, status) => adminService.updateUserStatus(userId, status).then(() => fetchUsers())}
          onResetPassword={handleResetPassword}
        />
      </div>
    </div>
  );
};

export default AdminUsersPage;
