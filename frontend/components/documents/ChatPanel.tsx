"use client";

import { Card, List, Input, Button, Space } from "antd";
import { useState } from "react";
import { ChatMessage } from "@/types";

interface ChatPanelProps {
  messages: ChatMessage[];
  onSend: (message: string) => void;
}

const ChatPanel = ({ messages, onSend }: ChatPanelProps) => {
  const [text, setText] = useState("");

  const handleSend = () => {
    if (!text.trim()) return;
    onSend(text.trim());
    setText("");
  };

  return (
    <Card title="实时聊天" size="small" bordered={false}>
      <List
        size="small"
        dataSource={[...messages].reverse()}
        style={{ maxHeight: 240, overflowY: "auto" }}
        renderItem={(item) => (
          <List.Item>
            <div>
              <strong>{item.username}</strong>
              <div style={{ color: "var(--text-muted)", fontSize: 12 }}>
                {new Date(item.timestamp).toLocaleTimeString()}
              </div>
              <div>{item.content}</div>
            </div>
          </List.Item>
        )}
      />
      <Space.Compact style={{ width: "100%", marginTop: 12 }}>
        <Input value={text} placeholder="输入聊天内容" onChange={(e) => setText(e.target.value)} onPressEnter={handleSend} />
        <Button type="primary" onClick={handleSend}>
          发送
        </Button>
      </Space.Compact>
    </Card>
  );
};

export default ChatPanel;
