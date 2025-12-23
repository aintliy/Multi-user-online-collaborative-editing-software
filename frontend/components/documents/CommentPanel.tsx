"use client";

import { Card, List, Avatar, Input, Button, Space } from "antd";
import { useCallback, useEffect, useState } from "react";
import { Comment } from "@/types";
import { commentsService } from "@/services/comments";
import dayjs from "dayjs";
import relativeTime from "dayjs/plugin/relativeTime";
import "dayjs/locale/zh-cn";

dayjs.extend(relativeTime);
dayjs.locale("zh-cn");

interface CommentPanelProps {
  documentId: number;
}

const CommentPanel = ({ documentId }: CommentPanelProps) => {
  const [comments, setComments] = useState<Comment[]>([]);
  const [loading, setLoading] = useState(false);
  const [content, setContent] = useState("");

  const fetchComments = useCallback(async () => {
    try {
      setLoading(true);
      const result = await commentsService.list(documentId);
      setComments(result.items);
    } finally {
      setLoading(false);
    }
  }, [documentId]);

  useEffect(() => {
    if (documentId) {
      void fetchComments();
    }
  }, [documentId, fetchComments]);

  const handleSubmit = async () => {
    if (!content.trim()) return;
    await commentsService.create({ documentId, content: content.trim() });
    setContent("");
    fetchComments();
  };

  return (
    <Card title="评论" size="small" bordered={false} extra={<Button type="link" onClick={fetchComments}>刷新</Button>}>
      <Space direction="vertical" style={{ width: "100%" }}>
        <Input.TextArea
          rows={3}
          value={content}
          placeholder="输入评论内容..."
          onChange={(e) => setContent(e.target.value)}
        />
        <Button type="primary" onClick={handleSubmit} disabled={!content.trim()}>
          发表评论
        </Button>
      </Space>
      <List
        loading={loading}
        dataSource={comments}
        style={{ marginTop: 16 }}
        locale={{ emptyText: "暂无评论" }}
        renderItem={(item) => (
          <List.Item>
            <List.Item.Meta
              avatar={<Avatar>{item.authorName?.[0]}</Avatar>}
              title={
                <Space>
                  <span>{item.authorName}</span>
                  <span style={{ color: "var(--text-muted)", fontSize: 12 }}>
                    {dayjs(item.createdAt).fromNow()}
                  </span>
                </Space>
              }
              description={item.content}
            />
          </List.Item>
        )}
      />
    </Card>
  );
};

export default CommentPanel;
