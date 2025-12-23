"use client";

import { ConfigProvider, App as AntdApp, theme } from "antd";
import zhCN from "antd/locale/zh_CN";
import { AuthProvider } from "@/components/providers/AuthProvider";
import { StyleProvider } from "@ant-design/cssinjs";

const AppProviders = ({ children }: { children: React.ReactNode }) => {
  return (
    <StyleProvider hashPriority="high">
      <ConfigProvider
        locale={zhCN}
        theme={{
          token: {
            colorPrimary: "#155eef",
            colorSuccess: "#2fb344",
            colorWarning: "#f39c12",
            colorError: "#ff4d4f",
            borderRadius: 12,
            fontSize: 14,
            fontFamily: "var(--font-display)",
          },
          algorithm: theme.defaultAlgorithm,
          components: {
            Layout: {
              headerBg: "#ffffff",
            },
          },
        }}
      >
        <AntdApp>
          <AuthProvider>{children}</AuthProvider>
        </AntdApp>
      </ConfigProvider>
    </StyleProvider>
  );
};

export default AppProviders;
