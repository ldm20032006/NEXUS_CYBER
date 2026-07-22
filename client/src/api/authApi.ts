import { getData, postData } from './apiClient'
import type {
  AuthResponse,
  CurrentUser,
  LoginRequest,
  RegisterGamerRequest,
  ForgotPasswordRequest,
  ResetPasswordRequest,
  ChangePasswordRequest,
} from '../types/api'

export const authApi = {
  register: (body: RegisterGamerRequest) =>
    postData<AuthResponse, RegisterGamerRequest>('/auth/register', body),
  login: (body: LoginRequest) =>
    postData<AuthResponse, LoginRequest>('/auth/login', body),
  currentUser: () => getData<CurrentUser>('/auth/me'),
  forgotPassword: (body: ForgotPasswordRequest) =>
    postData<void, ForgotPasswordRequest>('/auth/forgot-password', body),
  resetPassword: (body: ResetPasswordRequest) =>
    postData<void, ResetPasswordRequest>('/auth/reset-password', body),
  changePassword: (body: ChangePasswordRequest) =>
    postData<void, ChangePasswordRequest>('/auth/change-password', body),
  logout: (refreshToken: string) =>
    postData<void, { refreshToken: string }>('/auth/logout', { refreshToken }),
  logoutAll: () => postData<void>('/auth/logout-all'),
}
