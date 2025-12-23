import type { Metadata } from "next";
import { Space_Grotesk } from "next/font/google";
import "antd/dist/reset.css";
import "./globals.css";
import AppProviders from "@/components/providers/AppProviders";

const display = Space_Grotesk({
  subsets: ["latin"],
  weight: ["400", "500", "600", "700"],
  variable: "--font-display",
});

export const metadata: Metadata = {
  title: "多人协作文档工作台",
  description: "基于 Next.js + Ant Design 的多人在线协作编辑平台",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="zh-CN" className={display.variable}>
      <body>
        <AppProviders>{children}</AppProviders>
      </body>
    </html>
  );
}
