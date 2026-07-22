import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { mapApiError } from '../api/errors'
import { profileApi } from '../api/profileApi'
import { EmptyState, ErrorState, LoadingState, useToast } from '../components/ui'
import {
  gameProfileSchema,
  type GameProfileFormValues,
} from '../forms/profileSchemas'

export function GameProfilesPage() {
  const queryClient = useQueryClient()
  const { notify } = useToast()
  const [error, setError] = useState<string | null>(null)
  const profilesQuery = useQuery({
    queryKey: ['profile', 'game-profiles'],
    queryFn: profileApi.gameProfiles,
  })
  const form = useForm<GameProfileFormValues>({
    resolver: zodResolver(gameProfileSchema),
    defaultValues: {
      gameId: '',
      inGameName: '',
      rankId: '',
      preferredRoleId: '',
      secondaryRoleId: '',
      playStyle: '',
      shortDescription: '',
      visibleOnRadar: true,
    },
  })
  const mutation = useMutation({
    mutationFn: (values: GameProfileFormValues) =>
      profileApi.createGameProfile({
        gameId: values.gameId,
        inGameName: values.inGameName || undefined,
        rankId: values.rankId || undefined,
        preferredRoleId: values.preferredRoleId || undefined,
        secondaryRoleId: values.secondaryRoleId || undefined,
        playStyle: values.playStyle || undefined,
        shortDescription: values.shortDescription || undefined,
        visibleOnRadar: values.visibleOnRadar,
      }),
    onSuccess: () => {
      setError(null)
      notify('Game profile created', 'success')
      form.reset()
      void queryClient.invalidateQueries({ queryKey: ['profile', 'game-profiles'] })
    },
    onError: (apiError) => setError(mapApiError(apiError)),
  })

  if (profilesQuery.isLoading) {
    return <LoadingState label="Loading game profiles" />
  }
  if (profilesQuery.error) {
    return <ErrorState message={mapApiError(profilesQuery.error)} />
  }

  return (
    <section className="page-grid">
      <article className="panel">
        <h2>Game profiles</h2>
        {profilesQuery.data?.length ? (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Game</th>
                  <th>IGN</th>
                  <th>Rank</th>
                  <th>Role</th>
                  <th>Radar</th>
                </tr>
              </thead>
              <tbody>
                {profilesQuery.data.map((profile) => (
                  <tr key={profile.id}>
                    <td>{profile.gameName}</td>
                    <td>{profile.inGameName || '-'}</td>
                    <td>{profile.rankName || '-'}</td>
                    <td>{profile.preferredRoleName || '-'}</td>
                    <td>{profile.visibleOnRadar ? 'Visible' : 'Hidden'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <EmptyState title="No game profiles" />
        )}
      </article>
      <article className="panel">
        <h2>Add game profile</h2>
        <form className="form" onSubmit={form.handleSubmit((values) => mutation.mutate(values))}>
          {error ? <ErrorState message={error} /> : null}
          <label>
            Game ID
            <input {...form.register('gameId')} />
            <span>{form.formState.errors.gameId?.message}</span>
          </label>
          <label>
            In-game name
            <input {...form.register('inGameName')} />
            <span>{form.formState.errors.inGameName?.message}</span>
          </label>
          <label>
            Rank ID
            <input {...form.register('rankId')} />
            <span>{form.formState.errors.rankId?.message}</span>
          </label>
          <label>
            Preferred role ID
            <input {...form.register('preferredRoleId')} />
            <span>{form.formState.errors.preferredRoleId?.message}</span>
          </label>
          <label className="inline-check">
            <input type="checkbox" {...form.register('visibleOnRadar')} />
            Visible on Radar
          </label>
          <button className="button button-primary" type="submit" disabled={mutation.isPending}>
            Add profile
          </button>
        </form>
      </article>
    </section>
  )
}
