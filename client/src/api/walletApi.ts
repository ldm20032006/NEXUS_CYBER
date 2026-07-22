import { getData, postData } from './apiClient'
import type { TopUpRequest, TopUpResponse, Wallet, WalletTransaction } from '../types/api'

function requestId() {
  return crypto.randomUUID()
}

export const walletApi = {
  current: () => getData<Wallet>('/wallets/me'),
  transactions: () => getData<WalletTransaction[]>('/wallets/me/transactions'),
  topUp: (body: TopUpRequest) =>
    postData<TopUpResponse, TopUpRequest>('/payments/topups', body, {
      'Idempotency-Key': requestId(),
    }),
}
