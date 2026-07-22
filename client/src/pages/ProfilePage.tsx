import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { mapApiError } from '../api/errors'
import { profileApi } from '../api/profileApi'
import { ErrorState, LoadingState, useToast } from '../components/ui'
import { profileSchema, type ProfileFormValues } from '../forms/profileSchemas'

export function ProfilePage() {
  const queryClient = useQueryClient()
  const { notify } = useToast()
  const [error, setError] = useState<string | null>(null)
  const profileQuery = useQuery({
    queryKey: ['profile', 'me'],
    queryFn: profileApi.me,
  })
  const form = useForm<ProfileFormValues>({
    resolver: zodResolver(profileSchema),
    defaultValues: { nickname: '', avatarUrl: '', bio: '' },
  })

  useEffect(() => {
    if (profileQuery.data) {
      form.reset({
        nickname: profileQuery.data.nickname ?? '',
        avatarUrl: profileQuery.data.avatarUrl ?? '',
        bio: profileQuery.data.bio ?? '',
      })
    }
  }, [form, profileQuery.data])

  const mutation = useMutation({
    mutationFn: profileApi.updateMe,
    onSuccess: () => {
      setError(null)
      notify('Profile updated', 'success')
      void queryClient.invalidateQueries({ queryKey: ['profile', 'me'] })
    },
    onError: (apiError) => setError(mapApiError(apiError)),
  })

  if (profileQuery.isLoading) {
    return <LoadingState label="Loading profile" />
  }
  if (profileQuery.error) {
    return <ErrorState message={mapApiError(profileQuery.error)} />
  }

  return (
    <article className="panel narrow">
      <h2>Profile</h2>
      <form className="form" onSubmit={form.handleSubmit((values) => mutation.mutate(clean(values)))}>
        {error ? <ErrorState message={error} /> : null}
        <label>
          Nickname
          <input {...form.register('nickname')} />
          <span>{form.formState.errors.nickname?.message}</span>
        </label>
        <label>
          Avatar URL
          <input {...form.register('avatarUrl')} />
          <span>{form.formState.errors.avatarUrl?.message}</span>
        </label>
        <label>
          Bio
          <input {...form.register('bio')} />
          <span>{form.formState.errors.bio?.message}</span>
        </label>
        <button className="button button-primary" type="submit" disabled={mutation.isPending}>
          Save profile
        </button>
      </form>
    </article>
  )
}

function clean(values: ProfileFormValues) {
  return {
    nickname: values.nickname || undefined,
    avatarUrl: values.avatarUrl || undefined,
    bio: values.bio || undefined,
  }
}
