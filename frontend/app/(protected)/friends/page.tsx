"use client";

import PageHeader from "@/components/common/PageHeader";
import FriendPanels from "@/components/friends/FriendPanels";

const FriendsPage = () => {
  return (
    <div>
      <PageHeader title="好友与协作" description="管理好友关系与协作邀请" />
      <div className="page-section">
        <FriendPanels />
      </div>
    </div>
  );
};

export default FriendsPage;
