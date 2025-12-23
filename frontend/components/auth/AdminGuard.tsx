"use client";

import { Result, Button } from "antd";
import { useRouter } from "next/navigation";
import { useAuth } from "@/hooks/useAuth";

const AdminGuard = ({ children }: { children: React.ReactNode }) => {
  const { user } = useAuth();
  const router = useRouter();

  if (user && user.role !== "ADMIN") {
    return (
      <Result
        status="403"
        title="无权限"
        subTitle="该功能仅向管理员开放"
        extra={
          <Button type="primary" onClick={() => router.push("/documents")}>
            返回工作台
          </Button>
        }
      />
    );
  }

  return <>{children}</>;
};

export default AdminGuard;
