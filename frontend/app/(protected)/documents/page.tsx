"use client";

import { useCallback, useEffect, useState } from "react";
import { Modal, message, Row, Col } from "antd";
import PageHeader from "@/components/common/PageHeader";
import StatCard from "@/components/common/StatCard";
import DocumentFilters from "@/components/documents/DocumentFilters";
import DocumentTable from "@/components/documents/DocumentTable";
import DocumentCreateDrawer from "@/components/documents/DocumentCreateDrawer";
import DocumentImportModal from "@/components/documents/DocumentImportModal";
import { documentsService } from "@/services/documents";
import { Document, DocumentFilters as FilterValue, DocumentVisibility } from "@/types";
import { useRouter } from "next/navigation";

type DocumentFilterState = FilterValue & { visibility?: DocumentVisibility | "" };

const DocumentsPage = () => {
  const router = useRouter();
  const [documents, setDocuments] = useState<Document[]>([]);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [filters, setFilters] = useState<DocumentFilterState>({});
  const [createOpen, setCreateOpen] = useState(false);
  const [importOpen, setImportOpen] = useState(false);

  const fetchDocuments = useCallback(
    async (currentPage: number, currentSize: number, appliedFilters: DocumentFilterState) => {
      const { visibility, ...restFilters } = appliedFilters;
      const normalizedVisibility = visibility || undefined;
      try {
        setLoading(true);
        const result = await documentsService.list({ ...restFilters, page: currentPage, pageSize: currentSize, visibility: normalizedVisibility });
        setDocuments(result.items);
        setPage(result.page);
        setPageSize(result.pageSize);
        setTotal(result.total);
      } finally {
        setLoading(false);
      }
    },
    []
  );

  useEffect(() => {
    void fetchDocuments(1, pageSize, filters);
  }, [fetchDocuments, filters, pageSize]);

  const handleDelete = (record: Document) => {
    Modal.confirm({
      title: `确定删除《${record.title}》吗？`,
      content: "删除后将无法恢复",
      onOk: async () => {
        await documentsService.remove(record.id);
        message.success("已删除");
        fetchDocuments(page, pageSize, filters);
      },
    });
  };

  const handleClone = async (record: Document) => {
    await documentsService.clone(record.id);
    message.success("克隆成功");
    fetchDocuments(page, pageSize, filters);
  };

  return (
    <div>
      <PageHeader title="文档中心" description="集中管理你可访问的所有文档" />
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={6}><StatCard label="全部文档" value={total} /></Col>
        <Col span={6}><StatCard label="可编辑" value={documents.filter((doc) => doc.canEdit).length} accent="#22c55e" /></Col>
        <Col span={6}><StatCard label="公开文档" value={documents.filter((doc) => doc.visibility === "public").length} accent="#f97316" /></Col>
        <Col span={6}><StatCard label="克隆副本" value={documents.filter((doc) => doc.forkedFromId).length} accent="#6366f1" /></Col>
      </Row>
      <DocumentFilters value={filters} onChange={(next) => setFilters(next)} onCreate={() => setCreateOpen(true)} onImport={() => setImportOpen(true)} />
      <div className="page-section">
        <DocumentTable
          data={documents}
          loading={loading}
          total={total}
          page={page}
          pageSize={pageSize}
          onPageChange={(p, size) => {
            fetchDocuments(p, size, filters);
          }}
          onView={(record) => router.push(`/documents/${record.id}`)}
          onDelete={handleDelete}
          onClone={handleClone}
        />
      </div>
      <DocumentCreateDrawer
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        onSubmit={async (payload) => {
          await documentsService.create(payload);
          message.success("创建成功");
          fetchDocuments(page, pageSize, filters);
        }}
      />
      <DocumentImportModal
        open={importOpen}
        onClose={() => setImportOpen(false)}
        onImport={async (formData) => {
          await documentsService.importDocument(formData);
          fetchDocuments(page, pageSize, filters);
        }}
      />
    </div>
  );
};

export default DocumentsPage;
