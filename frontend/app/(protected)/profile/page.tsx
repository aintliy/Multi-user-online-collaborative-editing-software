"use client";

import PageHeader from "@/components/common/PageHeader";
import ProfileForm from "@/components/profile/ProfileForm";

const ProfilePage = () => {
  return (
    <div>
      <PageHeader title="个人中心" description="管理账号信息与安全设置" />
      <div className="page-section">
        <ProfileForm />
      </div>
    </div>
  );
};

export default ProfilePage;
