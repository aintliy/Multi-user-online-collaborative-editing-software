"use client";

import { Spin } from "antd";
import { usePathname, useRouter } from "next/navigation";
import { useEffect } from "react";
import { useAuth } from "@/hooks/useAuth";

const AuthGuard = ({ children }: { children: React.ReactNode }) => {
  const { user, loading } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (!loading && !user) {
      const redirect = encodeURIComponent(pathname || "/documents");
      router.replace(`/auth/login?redirect=${redirect}`);
    }
  }, [user, loading, pathname, router]);

  if (loading || (!user && typeof window !== "undefined")) {
    return (
      <div style={{ minHeight: "60vh", display: "flex", alignItems: "center", justifyContent: "center" }}>
        <Spin tip="加载中" />
      </div>
    );
  }

  return <>{children}</>;
};

export default AuthGuard;
