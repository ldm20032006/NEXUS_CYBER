import axios, {
  AxiosError,
  type AxiosInstance,
  type InternalAxiosRequestConfig,
} from 'axios'
import { env } from '../app/env'
import { useAuthStore } from '../auth/authStore'
import type { ApiResponse, AuthResponse } from '../types/api'

type RetryableRequest = InternalAxiosRequestConfig & {
  _retry?: boolean
  skipAuthRefresh?: boolean
}

let refreshPromise: Promise<string> | null = null

export const httpClient: AxiosInstance = axios.create({
  baseURL: env.VITE_API_BASE_URL,
  timeout: 15_000,
  headers: {
    'Content-Type': 'application/json',
  },
})

httpClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken
  if (token && !config.headers.Authorization) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

httpClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const request = error.config as RetryableRequest | undefined
    if (!request || request.skipAuthRefresh || request._retry) {
      return Promise.reject(error)
    }
    if (error.response?.status !== 401) {
      return Promise.reject(error)
    }

    const refreshToken = useAuthStore.getState().refreshToken
    if (!refreshToken) {
      useAuthStore.getState().clearAuth()
      return Promise.reject(error)
    }

    request._retry = true
    try {
      const accessToken = await refreshAccessToken(refreshToken)
      request.headers.Authorization = `Bearer ${accessToken}`
      return httpClient(request)
    } catch (refreshError) {
      useAuthStore.getState().clearAuth()
      return Promise.reject(refreshError)
    }
  },
)

async function refreshAccessToken(refreshToken: string): Promise<string> {
  if (!refreshPromise) {
    refreshPromise = httpClient
      .post<ApiResponse<AuthResponse>>(
        '/auth/refresh-token',
        { refreshToken },
        { skipAuthRefresh: true } as RetryableRequest,
      )
      .then((response) => {
        useAuthStore.getState().setAuth(response.data.data)
        return response.data.data.accessToken
      })
      .finally(() => {
        refreshPromise = null
      })
  }
  return refreshPromise
}
