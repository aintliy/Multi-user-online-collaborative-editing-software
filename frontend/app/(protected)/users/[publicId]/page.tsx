"use client";

import { useEffect, useState } from "react";
import { Card, List, Tag } from "antd";
import { notFound } from "next/navigation";
import PageHeader from "@/components/common/PageHeader";
import { usersService } from "@/services/users";
import { Document, User } from "@/types";

interface UserProfilePageProps {
  params: { publicId: string };
}

const UserProfilePage = ({ params }: UserProfilePageProps) => {
  const [user, setUser] = useState<User | null>(null);
  const [documents, setDocuments] = useState<Document[]>([]);

  useEffect(() => {
    const load = async () => {
      try {
        const profile = await usersService.getByPublicId(params.publicId);
        setUser(profile);
        const repoResult = await usersService.getUserRepos(params.publicId, 1, 20);
        setDocuments(repoResult.items);
      } catch {
        notFound();
      }
    };
    load();
  }, [params.publicId]);

  if (!user) {
    return null;
  }

  return (
    <div>
      <PageHeader title={`${user.username} 的主页`} description={user.profile} breadcrumb={[{ label: "发现", href: "/explore" }, { label: user.username }]} />
      <div className="page-section">
        <Card>
          <p>Public ID：{user.publicId}</p>
          <p>邮箱：{user.email}</p>
        </Card>
      </div>
      <div className="page-section">
        <List
          header="公开文档"
          dataSource={documents}
          renderItem={(doc) => (
            <List.Item actions={[<a key="view" href={`/documents/${doc.id}`}>查看</a>]}> 
              <List.Item.Meta title={doc.title} description={doc.ownerName} />
              <Tag color={doc.visibility === "public" ? "green" : "gold"}>{doc.visibility === "public" ? "公开" : "私有"}</Tag>
            </List.Item>
          )}
        />
      </div>
    </div>
  );
};

export default UserProfilePage;
