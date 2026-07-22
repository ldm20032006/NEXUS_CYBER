import { httpClient } from './httpClient'
import type { ApiResponse } from '../types/api'

export async function getData<T>(url: string, params?: unknown): Promise<T> {
  const response = await httpClient.get<ApiResponse<T>>(url, { params })
  return response.data.data
}

export async function postData<TResponse, TBody = unknown>(
  url: string,
  body?: TBody,
  headers?: Record<string, string>,
): Promise<TResponse> {
  const response = await httpClient.post<ApiResponse<TResponse>>(url, body, {
    headers,
  })
  return response.data.data
}

export async function putData<TResponse, TBody = unknown>(
  url: string,
  body?: TBody,
): Promise<TResponse> {
  const response = await httpClient.put<ApiResponse<TResponse>>(url, body)
  return response.data.data
}

export async function patchData<TResponse, TBody = unknown>(
  url: string,
  body?: TBody,
): Promise<TResponse> {
  const response = await httpClient.patch<ApiResponse<TResponse>>(url, body)
  return response.data.data
}

export async function deleteData<TResponse>(url: string): Promise<TResponse> {
  const response = await httpClient.delete<ApiResponse<TResponse>>(url)
  return response.data.data
}
