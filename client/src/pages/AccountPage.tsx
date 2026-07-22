import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { authApi } from '../api/authApi'
import { mapApiError } from '../api/errors'
import { ErrorState, LoadingState, useToast } from '../components/ui'
import {
  changePasswordSchema,
  type ChangePasswordFormValues,
} from '../forms/authSchemas'

export function AccountPage() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['auth', 'me'],
    queryFn: authApi.currentUser,
  })

  if (isLoading) {
    return <LoadingState label="Loading account" />
  }
  if (error) {
    return <ErrorState message={mapApiError(error)} />
  }

  return (
    <section className="page-grid">
      <article className="panel">
        <h2>Current user</h2>
        <dl className="details">
          <dt>Name</dt>
          <dd>{data?.displayName || data?.fullName}</dd>
          <dt>Email</dt>
          <dd>{data?.email || '-'}</dd>
          <dt>Phone</dt>
          <dd>{data?.phone || '-'}</dd>
          <dt>Roles</dt>
          <dd>{data?.roles.join(', ') || '-'}</dd>
          <dt>Branch</dt>
          <dd>{data?.branchId || '-'}</dd>
        </dl>
      </article>
      <ChangePasswordPanel />
    </section>
  )
}

function ChangePasswordPanel() {
  const { notify } = useToast()
  const [error, setError] = useState<string | null>(null)
  const form = useForm<ChangePasswordFormValues>({
    resolver: zodResolver(changePasswordSchema),
    defaultValues: { currentPassword: '', newPassword: '' },
  })
  const mutation = useMutation({
    mutationFn: authApi.changePassword,
    onSuccess: () => {
      setError(null)
      notify('Password changed', 'success')
      form.reset()
    },
    onError: (apiError) => setError(mapApiError(apiError)),
  })

  return (
    <article className="panel">
      <h2>Change password</h2>
      <form className="form" onSubmit={form.handleSubmit((values) => mutation.mutate(values))}>
        {error ? <ErrorState message={error} /> : null}
        <label>
          Current password
          <input type="password" autoComplete="current-password" {...form.register('currentPassword')} />
          <span>{form.formState.errors.currentPassword?.message}</span>
        </label>
        <label>
          New password
          <input type="password" autoComplete="new-password" {...form.register('newPassword')} />
          <span>{form.formState.errors.newPassword?.message}</span>
        </label>
        <button className="button button-primary" type="submit" disabled={mutation.isPending}>
          Change password
        </button>
      </form>
    </article>
  )
}
