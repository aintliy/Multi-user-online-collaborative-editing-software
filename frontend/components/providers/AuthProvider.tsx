"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { message } from "antd";
import { authService, LoginPayload } from "@/services/auth";
import { User } from "@/types";
import { tokenStorage } from "@/utils/tokenStorage";

interface AuthContextValue {
  user: User | null;
  token: string | null;
  loading: boolean;
  login: (payload: LoginPayload) => Promise<User>;
  logout: () => void;
  refresh: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export const AuthProvider = ({ children }: { children: React.ReactNode }) => {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const fetchProfile = useCallback(async () => {
    try {
      const profile = await authService.getProfile();
      setUser(profile);
    } catch {
      tokenStorage.clear();
      setUser(null);
      setToken(null);
    }
  }, []);

  useEffect(() => {
    let active = true;

    const bootstrap = async () => {
      const storedToken = tokenStorage.getToken();
      if (!storedToken) {
        if (active) {
          setUser(null);
          setToken(null);
          setLoading(false);
        }
        return;
      }

      if (active) {
        setToken(storedToken);
      }

      try {
        await fetchProfile();
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    };

    void bootstrap();

    return () => {
      active = false;
    };
  }, [fetchProfile]);

  const login = useCallback(async (payload: LoginPayload) => {
    const data = await authService.login(payload);
    tokenStorage.setToken(data.token);
    setToken(data.token);
    setUser(data.user);
    message.success("登录成功");
    return data.user;
  }, []);

  const logout = useCallback(() => {
    tokenStorage.clear();
    setUser(null);
    setToken(null);
  }, []);

  const refresh = useCallback(async () => {
    await fetchProfile();
  }, [fetchProfile]);

  const value = useMemo(
    () => ({ user, token, loading, login, logout, refresh }),
    [user, token, loading, login, logout, refresh]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuthContext = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuthContext must be used within AuthProvider");
  }
  return ctx;
};
