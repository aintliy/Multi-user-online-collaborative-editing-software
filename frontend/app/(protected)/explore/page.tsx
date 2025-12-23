"use client";

import { useCallback, useEffect, useState } from "react";
import { Card, Col, Input, Row, Tag } from "antd";
import PageHeader from "@/components/common/PageHeader";
import { documentsService } from "@/services/documents";
import { Document } from "@/types";
import { useRouter } from "next/navigation";

const ExplorePage = () => {
  const router = useRouter();
  const [keyword, setKeyword] = useState("");
  const [documents, setDocuments] = useState<Document[]>([]);

  const fetchPublicDocs = useCallback(
    async (query?: string) => {
      const keywordValue = query ?? "";
      const result = await documentsService.list({ keyword: keywordValue, page: 1, pageSize: 50, visibility: "public" });
      setDocuments(result.items);
    },
    []
  );

  useEffect(() => {
    void (async () => {
      await fetchPublicDocs();
    })();
  }, [fetchPublicDocs]);

  return (
    <div>
      <PageHeader title="发现" description="浏览公开的协作文档" />
      <div className="page-section">
        <Input.Search placeholder="按关键字搜索公开文档" value={keyword} onChange={(e) => setKeyword(e.target.value)} onSearch={(value) => fetchPublicDocs(value)} allowClear />
      </div>
      <Row gutter={[16, 16]}>
        {documents.map((doc) => (
          <Col span={8} key={doc.id}>
            <Card hoverable onClick={() => router.push(`/documents/${doc.id}`)} style={{ borderRadius: 18 }}>
              <Card.Meta title={doc.title} description={doc.ownerName} />
              <div style={{ marginTop: 12 }}>
                <Tag color="green">公开</Tag>
                {doc.tags && <Tag>{doc.tags}</Tag>}
              </div>
            </Card>
          </Col>
        ))}
      </Row>
    </div>
  );
};

export default ExplorePage;
