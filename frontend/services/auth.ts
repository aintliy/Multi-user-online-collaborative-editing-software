import { apiClient } from "@/lib/apiClient";
import { LoginResponse, User } from "@/types";

export interface LoginPayload {
  email: string;
  password: string;
}

export interface RegisterPayload {
  email: string;
  username: string;
  password: string;
  confirmPassword: string;
  verificationCode: string;
  phone?: string;
}

export interface ForgotPasswordPayload {
  email: string;
}

export interface ResetPasswordPayload {
  token: string;
  newPassword: string;
}

export interface UpdateProfilePayload {
  username?: string;
  phone?: string;
  profile?: string;
}

export const authService = {
  login(payload: LoginPayload) {
    return apiClient.post<LoginResponse>("/api/auth/login", payload);
  },
  register(payload: RegisterPayload) {
    const { confirmPassword, ...rest } = payload;
    if (payload.password !== confirmPassword) {
      return Promise.reject(new Error("两次输入的密码不一致"));
    }
    return apiClient.post<User>("/api/auth/register", rest);
  },
  sendVerificationCode(email: string) {
    return apiClient.post<void>("/api/auth/send-verification-code", { email });
  },
  forgotPassword(payload: ForgotPasswordPayload) {
    return apiClient.post<void>("/api/auth/forgot-password", payload);
  },
  resetPassword(payload: ResetPasswordPayload) {
    return apiClient.post<void>("/api/auth/reset-password", payload);
  },
  getProfile() {
    return apiClient.get<User>("/api/auth/me");
  },
  updateProfile(payload: UpdateProfilePayload) {
    return apiClient.put<void>("/api/auth/profile", payload);
  },
};
