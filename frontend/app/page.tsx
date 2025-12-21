'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/contexts/AuthContext';
import { Button, Spin } from 'antd';

export default function Home() {
  const router = useRouter();
  const { user, isLoading } = useAuth();

  useEffect(() => {
    if (!isLoading && user) {
      router.push('/documents');
    }
  }, [user, isLoading, router]);

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <Spin size="large" />
      </div>
    );
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100">
      <div className="text-center">
        <h1 className="text-5xl font-bold text-gray-800 mb-4">
          多人在线协作编辑系统
        </h1>
        <p className="text-xl text-gray-600 mb-8">
          实时协作，高效编辑
        </p>
        <div className="space-x-4">
          <Button
            type="primary"
            size="large"
            onClick={() => router.push('/login')}
          >
            登录
          </Button>
          <Button
            size="large"
            onClick={() => router.push('/register')}
          >
            注册
          </Button>
        </div>
      </div>
    </div>
  );
}
