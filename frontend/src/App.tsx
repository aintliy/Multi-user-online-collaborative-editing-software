import React, { useEffect } from 'react';
import { message } from 'antd';
import AppRouter from './router';
import { useAuthStore } from './store/useAuthStore';
import { authApi } from './api';
import './App.scss';

const App: React.FC = () => {
  const { token, setAuth, logout } = useAuthStore();

  useEffect(() => {
    // 如果有 token，尝试获取用户信息
    if (token) {
      authApi.getCurrentUser()
        .then((user) => {
          setAuth(token, user);
        })
        .catch(() => {
          logout();
          message.warning('登录已过期，请重新登录');
        });
    }
  }, []);

  return <AppRouter />;
};

export default App;
