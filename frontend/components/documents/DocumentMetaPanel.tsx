"use client";

import { Card, Descriptions, Tag } from "antd";
import dayjs from "dayjs";
import { Document } from "@/types";

interface DocumentMetaPanelProps {
  document: Document;
}

const DocumentMetaPanel = ({ document }: DocumentMetaPanelProps) => {
  return (
    <Card title="文档信息" size="small" bordered={false}>
      <Descriptions column={1} size="small">
        <Descriptions.Item label="所有者">{document.ownerName}</Descriptions.Item>
        <Descriptions.Item label="文档类型">{document.docType?.toUpperCase()}</Descriptions.Item>
        <Descriptions.Item label="可见性">
          <Tag color={document.visibility === "public" ? "green" : "gold"}>
            {document.visibility === "public" ? "公开" : "私有"}
          </Tag>
        </Descriptions.Item>
        <Descriptions.Item label="创建时间">
          {document.createdAt && dayjs(document.createdAt).format("YYYY-MM-DD HH:mm")}
        </Descriptions.Item>
        <Descriptions.Item label="最后更新">
          {document.updatedAt && dayjs(document.updatedAt).format("YYYY-MM-DD HH:mm")}
        </Descriptions.Item>
        {document.tags && <Descriptions.Item label="标签">{document.tags}</Descriptions.Item>}
      </Descriptions>
    </Card>
  );
};

export default DocumentMetaPanel;
