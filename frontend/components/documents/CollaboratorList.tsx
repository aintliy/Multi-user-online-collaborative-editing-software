"use client";

import { Card, List, Avatar, Space, Tag, Select } from "antd";
import { Collaborator, CollaboratorRole, OnlineUser } from "@/types";

interface CollaboratorListProps {
  onlineUsers: OnlineUser[];
  collaborators: Collaborator[];
  editable?: boolean;
  onRoleChange?: (id: number, role: CollaboratorRole) => void;
}

const roleOptions = [
  { label: "查看者", value: "VIEWER" },
  { label: "编辑者", value: "EDITOR" },
];

const CollaboratorList = ({ onlineUsers, collaborators, editable, onRoleChange }: CollaboratorListProps) => {
  return (
    <Space direction="vertical" style={{ width: "100%" }} size="middle">
      <Card title="在线协作者" size="small" bordered={false}>
        <List
          dataSource={onlineUsers}
          locale={{ emptyText: "暂无在线用户" }}
          renderItem={(user) => (
            <List.Item>
              <Space>
                <Avatar style={{ backgroundColor: user.color }} size="small">
                  {user.username?.[0] || "U"}
                </Avatar>
                <span>{user.username}</span>
              </Space>
            </List.Item>
          )}
        />
      </Card>
      <Card title="协作者列表" size="small" bordered={false}>
        <List
          dataSource={collaborators}
          locale={{ emptyText: "暂无协作者" }}
          renderItem={(collaborator) => (
            <List.Item extra={editable && (
                <Select
                  value={collaborator.role}
                  options={roleOptions}
                  size="small"
                  style={{ width: 120 }}
                  onChange={(role) => onRoleChange?.(collaborator.id, role as CollaboratorRole)}
                />
              )}
            >
              <List.Item.Meta
                avatar={<Avatar src={collaborator.avatarUrl}>{collaborator.username?.[0]}</Avatar>}
                title={collaborator.username}
                description={<Tag color={collaborator.role === "EDITOR" ? "green" : "gold"}>{collaborator.role === "EDITOR" ? "编辑" : "查看"}</Tag>}
              />
            </List.Item>
          )}
        />
      </Card>
    </Space>
  );
};

export default CollaboratorList;
