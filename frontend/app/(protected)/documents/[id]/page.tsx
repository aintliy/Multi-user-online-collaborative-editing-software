"use client";

import { useCallback, useEffect, useState } from "react";
import { Button, Col, Row, Space, message } from "antd";
import { notFound } from "next/navigation";
import PageHeader from "@/components/common/PageHeader";
import DocumentEditor from "@/components/documents/DocumentEditor";
import CollaboratorList from "@/components/documents/CollaboratorList";
import CommentPanel from "@/components/documents/CommentPanel";
import ChatPanel from "@/components/documents/ChatPanel";
import DocumentMetaPanel from "@/components/documents/DocumentMetaPanel";
import VersionTimeline from "@/components/documents/VersionTimeline";
import { documentsService } from "@/services/documents";
import { Document, Collaborator, DocumentVersion } from "@/types";
import { useDocumentChannel } from "@/hooks/useDocumentChannel";

interface DocumentDetailPageProps {
  params: { id: string };
}

const DocumentDetailPage = ({ params }: DocumentDetailPageProps) => {
  const docId = Number(params.id);
  const [document, setDocument] = useState<Document | null>(null);
  const [content, setContent] = useState("");
  const [versions, setVersions] = useState<DocumentVersion[]>([]);
  const [collaborators, setCollaborators] = useState<Collaborator[]>([]);
  const { onlineUsers, chatMessages, sendChat, sendEdit } = useDocumentChannel(docId);

  const loadDocument = useCallback(async () => {
    try {
      const data = await documentsService.get(docId);
      setDocument(data);
      setContent(data.content || "");
    } catch {
      notFound();
    }
  }, [docId]);

  const loadVersions = useCallback(async () => {
    const result = await documentsService.getVersions(docId);
    setVersions(result);
  }, [docId]);

  const loadCollaborators = useCallback(async () => {
    const list = await documentsService.getCollaborators(docId);
    setCollaborators(list);
  }, [docId]);

  useEffect(() => {
    if (Number.isNaN(docId)) return;
    void (async () => {
      await Promise.all([loadDocument(), loadVersions(), loadCollaborators()]);
    })();
  }, [docId, loadCollaborators, loadDocument, loadVersions]);

  const handleCommit = async () => {
    if (!document?.canEdit) return;
    await documentsService.commit(docId, {
      content,
      commitMessage: `更新于 ${new Date().toLocaleString()}`,
    });
    message.success("已提交新版本");
    await Promise.all([loadDocument(), loadVersions()]);
  };

  if (!document) {
    return null;
  }

  return (
    <div>
      <PageHeader
        title={document.title}
        description={`所有者：${document.ownerName}`}
        breadcrumb={[{ label: "文档中心", href: "/documents" }, { label: document.title }]}
        extra={
          <Space>
            {document.canEdit && (
              <Button type="primary" onClick={handleCommit}>
                提交更新
              </Button>
            )}
          </Space>
        }
      />
      <Row gutter={24}>
        <Col span={16}>
          <div className="page-section">
            <DocumentEditor value={content} onChange={setContent} readOnly={!document.canEdit} onRealtimeEdit={document.canEdit ? (op) => sendEdit(op) : undefined} />
          </div>
          <div className="page-section">
            <CommentPanel documentId={document.id} />
          </div>
        </Col>
        <Col span={8}>
          <Space direction="vertical" size={24} style={{ width: "100%" }}>
            <DocumentMetaPanel document={document} />
            <CollaboratorList onlineUsers={onlineUsers} collaborators={collaborators} />
            <ChatPanel messages={chatMessages} onSend={sendChat} />
            <VersionTimeline
              versions={versions}
              onRollback={(version) =>
                documentsService.rollback(docId, version).then(async () => {
                  await Promise.all([loadDocument(), loadVersions()]);
                })
              }
            />
          </Space>
        </Col>
      </Row>
    </div>
  );
};

export default DocumentDetailPage;
