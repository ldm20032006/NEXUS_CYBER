import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { mapApiError } from '../api/errors'
import { walletApi } from '../api/walletApi'
import { EmptyState, ErrorState, LoadingState, useToast } from '../components/ui'

export function WalletPage() {
  const queryClient = useQueryClient()
  const toast = useToast()
  const [topUpAmount, setTopUpAmount] = useState('50000')
  const walletQuery = useQuery({
    queryKey: ['wallet', 'current'],
    queryFn: walletApi.current,
  })
  const transactionsQuery = useQuery({
    queryKey: ['wallet', 'transactions'],
    queryFn: walletApi.transactions,
  })
  const topUpMutation = useMutation({
    mutationFn: () => walletApi.topUp({ amount: topUpAmount }),
    onSuccess: async (response) => {
      await queryClient.invalidateQueries({ queryKey: ['wallet', 'transactions'] })
      toast.notify(`Top-up created with status ${response.status}. Complete checkout to credit wallet.`, 'success')
      if (response.checkoutUrl) {
        window.open(response.checkoutUrl, '_blank', 'noopener,noreferrer')
      }
    },
    onError: (error) => toast.notify(mapApiError(error), 'error'),
  })

  if (walletQuery.isLoading || transactionsQuery.isLoading) {
    return <LoadingState label="Loading wallet" />
  }
  if (walletQuery.error) {
    return <ErrorState message={mapApiError(walletQuery.error)} />
  }
  if (transactionsQuery.error) {
    return <ErrorState message={mapApiError(transactionsQuery.error)} />
  }

  return (
    <section className="page-grid">
      <article className="panel">
        <h2>Wallet overview</h2>
        <p className="metric">
          {walletQuery.data?.balance ?? '0'} {walletQuery.data?.currency ?? 'VND'}
        </p>
        <label className="field">
          Top-up amount
          <input value={topUpAmount} onChange={(event) => setTopUpAmount(event.target.value)} />
        </label>
        <button
          type="button"
          className="button button-primary"
          onClick={() => topUpMutation.mutate()}
          disabled={topUpMutation.isPending || Number(topUpAmount) < 1}
        >
          Create top-up
        </button>
        <p className="help-text">
          Top-up stays pending until the payment webhook confirms it. The UI never marks payment as
          successful by itself.
        </p>
      </article>
      <article className="panel">
        <h2>Transactions</h2>
        {transactionsQuery.data?.length ? (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Type</th>
                  <th>Amount</th>
                  <th>Balance</th>
                  <th>Created</th>
                </tr>
              </thead>
              <tbody>
                {transactionsQuery.data.map((transaction) => (
                  <tr key={transaction.id}>
                    <td>{transaction.type}</td>
                    <td>{transaction.amount}</td>
                    <td>{transaction.balanceAfter}</td>
                    <td>{new Date(transaction.createdAt).toLocaleString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <EmptyState title="No transactions" />
        )}
      </article>
    </section>
  )
}
