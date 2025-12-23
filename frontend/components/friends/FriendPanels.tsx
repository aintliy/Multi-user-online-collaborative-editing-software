"use client";

import { Card, List, Avatar, Button, Input, Tabs, Space } from "antd";
import { useCallback, useEffect, useState } from "react";
import { User } from "@/types";
import { usersService } from "@/services/users";

const FriendPanels = () => {
  const [friends, setFriends] = useState<User[]>([]);
  const [requests, setRequests] = useState<User[]>([]);
  const [searchResult, setSearchResult] = useState<User[]>([]);
  const [keyword, setKeyword] = useState("");

  const refresh = useCallback(async () => {
    const [friendRes, requestRes] = await Promise.all([usersService.getFriends(), usersService.getFriendRequests()]);
    setFriends(friendRes);
    setRequests(requestRes);
  }, []);

  useEffect(() => {
    void (async () => {
      await refresh();
    })();
  }, [refresh]);

  const handleSearch = async () => {
    if (!keyword.trim()) return;
    const result = await usersService.search({ keyword: keyword.trim(), limit: 5 });
    setSearchResult(result);
  };

  return (
    <Tabs
      defaultActiveKey="friends"
      items={[
        {
          key: "friends",
          label: `好友列表 (${friends.length})`,
          children: (
            <Card bordered={false}>
              <List
                dataSource={friends}
                locale={{ emptyText: "暂无好友" }}
                renderItem={(item) => (
                  <List.Item
                    extra={
                      <Button danger size="small" onClick={() => usersService.removeFriend(item.id).then(refresh)}>
                        移除
                      </Button>
                    }
                  >
                    <List.Item.Meta
                      avatar={<Avatar src={item.avatarUrl}>{item.username?.[0]}</Avatar>}
                      title={item.username}
                      description={item.profile}
                    />
                  </List.Item>
                )}
              />
            </Card>
          ),
        },
        {
          key: "requests",
          label: `待处理请求 (${requests.length})`,
          children: (
            <Card bordered={false}>
              <List
                dataSource={requests}
                locale={{ emptyText: "暂无请求" }}
                renderItem={(item) => (
                  <List.Item
                    extra={
                      <Space>
                        <Button type="link" onClick={() => usersService.acceptFriendRequest(item.id).then(refresh)}>
                          接受
                        </Button>
                        <Button type="link" danger onClick={() => usersService.rejectFriendRequest(item.id).then(refresh)}>
                          拒绝
                        </Button>
                      </Space>
                    }
                  >
                    <List.Item.Meta
                      avatar={<Avatar src={item.avatarUrl}>{item.username?.[0]}</Avatar>}
                      title={item.username}
                      description={item.profile}
                    />
                  </List.Item>
                )}
              />
            </Card>
          ),
        },
        {
          key: "search",
          label: "搜索用户",
          children: (
            <Card bordered={false}>
              <Space style={{ marginBottom: 16 }}>
                <Input value={keyword} placeholder="输入用户名或公共ID" onChange={(e) => setKeyword(e.target.value)} onPressEnter={handleSearch} />
                <Button type="primary" onClick={handleSearch}>
                  搜索
                </Button>
              </Space>
              <List
                dataSource={searchResult}
                locale={{ emptyText: "暂无结果" }}
                renderItem={(item) => (
                  <List.Item
                    extra={
                      <Button type="link" onClick={() => usersService.sendFriendRequest(item.id).then(() => setKeyword(""))}>
                        添加
                      </Button>
                    }
                  >
                    <List.Item.Meta
                      avatar={<Avatar src={item.avatarUrl}>{item.username?.[0]}</Avatar>}
                      title={item.username}
                      description={`PublicId: ${item.publicId}`}
                    />
                  </List.Item>
                )}
              />
            </Card>
          ),
        },
      ]}
    />
  );
};

export default FriendPanels;
