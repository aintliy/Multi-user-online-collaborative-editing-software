"use client";

import { Input, Select, Space, Button } from "antd";
import type { DocumentFilters as DocumentFilterValues, DocumentVisibility } from "@/types";

interface DocumentFiltersProps {
  value: DocumentFilterValues & { visibility?: DocumentVisibility | "" };
  onChange: (value: DocumentFilterValues & { visibility?: DocumentVisibility | "" }) => void;
  onCreate: () => void;
  onImport: () => void;
}

const visibilityOptions = [
  { label: "全部可见性", value: "" },
  { label: "公开文档", value: "public" },
  { label: "私有文档", value: "private" },
];

const DocumentFilters = ({ value, onChange, onCreate, onImport }: DocumentFiltersProps) => {
  return (
    <div className="page-section">
      <Space wrap style={{ width: "100%", justifyContent: "space-between" }}>
        <Space wrap>
          <Input.Search
            allowClear
            placeholder="搜索标题 / 标签"
            value={value.keyword}
            onChange={(e) => onChange({ ...value, keyword: e.target.value })}
            style={{ width: 260 }}
          />
          <Select<DocumentVisibility | "">
            value={value.visibility || ""}
            options={visibilityOptions}
            style={{ width: 160 }}
            onChange={(visibility) => onChange({ ...value, visibility: visibility || "" })}
          />
        </Space>
        <Space>
          <Button onClick={onImport}>导入文档</Button>
          <Button type="primary" onClick={onCreate}>
            新建文档
          </Button>
        </Space>
      </Space>
    </div>
  );
};

export default DocumentFilters;
