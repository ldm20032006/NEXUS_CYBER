import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { mapApiError } from '../api/errors'
import { sessionApi } from '../api/sessionApi'
import { ConfirmDialog, EmptyState, ErrorState, LoadingState, useToast } from '../components/ui'
import { hasPartialFailure, isSessionEvent } from '../features/session/sessionEvents'
import { webSocketClient } from '../websocket/webSocketClient'

export function CurrentSessionPage() {
  const queryClient = useQueryClient()
  const toast = useToast()
  const [endDialogOpen, setEndDialogOpen] = useState(false)
  const [endReason, setEndReason] = useState('')
  const sessionQuery = useQuery({
    queryKey: ['sessions', 'current'],
    queryFn: sessionApi.current,
    refetchInterval: 5000,
  })

  const applyPreferenceMutation = useMutation({
    mutationFn: (stationId: string) => sessionApi.applyStationPreference(stationId),
    onError: (error) => toast.notify(mapApiError(error), 'error'),
  })

  const endSessionMutation = useMutation({
    mutationFn: (sessionId: string) => sessionApi.end(sessionId, { reason: endReason || undefined }),
    onSuccess: async () => {
      setEndDialogOpen(false)
      setEndReason('')
      await queryClient.invalidateQueries({ queryKey: ['sessions', 'current'] })
      await queryClient.invalidateQueries({ queryKey: ['sessions'] })
      toast.notify('Session ended.', 'success')
    },
    onError: (error) => toast.notify(mapApiError(error), 'error'),
  })

  useEffect(() => {
    const subscription = webSocketClient.subscribe<unknown>('/user/queue/notifications', (payload) => {
      if (isSessionEvent(payload)) {
        void queryClient.invalidateQueries({ queryKey: ['sessions', 'current'] })
      }
    })
    return () => subscription.unsubscribe()
  }, [queryClient])

  if (sessionQuery.isLoading) {
    return <LoadingState label="Loading current session" />
  }
  if (sessionQuery.error) {
    return <ErrorState message={mapApiError(sessionQuery.error)} />
  }
  if (!sessionQuery.data) {
    return <EmptyState title="No active session" description="Scan a station QR to start." />
  }

  const session = sessionQuery.data
  const commandResult = applyPreferenceMutation.data
  const partialFailure = hasPartialFailure(commandResult)

  return (
    <div className="page-grid">
      <article className="panel">
        <div className="panel-heading">
          <div>
            <p className="eyebrow">Live session</p>
            <h2>Current session</h2>
          </div>
          <span className="status-pill status-success">{session.status}</span>
        </div>
        <dl className="details">
          <dt>Station</dt>
          <dd>{session.stationId}</dd>
          <dt>Started</dt>
          <dd>{session.startedAt ? new Date(session.startedAt).toLocaleString() : '-'}</dd>
          <dt>Estimated cost</dt>
          <dd>{session.estimatedCost ?? '-'}</dd>
          <dt>Actual cost</dt>
          <dd>{session.actualCost ?? '-'}</dd>
        </dl>
        <label className="field">
          End reason
          <input
            value={endReason}
            onChange={(event) => setEndReason(event.target.value)}
            placeholder="Optional"
          />
        </label>
        <div className="action-row">
          <button
            type="button"
            className="button button-secondary"
            onClick={() => applyPreferenceMutation.mutate(session.stationId)}
            disabled={applyPreferenceMutation.isPending}
          >
            Apply station preference
          </button>
          <button type="button" className="button button-danger" onClick={() => setEndDialogOpen(true)}>
            End session
          </button>
        </div>
      </article>

      <article className="panel">
        <div className="panel-heading">
          <div>
            <p className="eyebrow">Smart Station</p>
            <h2>Command progress</h2>
          </div>
          {commandResult ? (
            <span className={`status-pill ${partialFailure ? 'status-warning' : 'status-success'}`}>
              {commandResult.status}
            </span>
          ) : null}
        </div>
        {!commandResult && !applyPreferenceMutation.isPending ? (
          <EmptyState
            title="No command batch yet"
            description="Apply your station preference to send supported desk, chair, light, and device commands."
          />
        ) : null}
        {applyPreferenceMutation.isPending ? <LoadingState label="Applying station preference" /> : null}
        {commandResult ? (
          <section className="command-progress">
            <div className="progress-summary">
              <strong>{commandResult.success}</strong>
              <span>success</span>
              <strong>{commandResult.failed}</strong>
              <span>failed</span>
              <strong>{commandResult.skipped}</strong>
              <span>skipped</span>
            </div>
            {partialFailure ? (
              <ErrorState message="Some Smart Station commands failed or were skipped. Session remains active." />
            ) : null}
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Command</th>
                    <th>Status</th>
                    <th>Attempt</th>
                    <th>Message</th>
                  </tr>
                </thead>
                <tbody>
                  {commandResult.commands.map((command) => (
                    <tr key={command.id}>
                      <td>{command.commandType}</td>
                      <td>{command.status}</td>
                      <td>
                        {command.attemptCount}/{command.maxAttempts}
                      </td>
                      <td>{command.resultMessage ?? '-'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        ) : null}
      </article>

      <ConfirmDialog
        open={endDialogOpen}
        title="End session"
        message="The backend will finalize billing and release the station."
        confirmLabel={endSessionMutation.isPending ? 'Ending...' : 'End session'}
        onCancel={() => setEndDialogOpen(false)}
        onConfirm={() => endSessionMutation.mutate(session.id)}
      />
    </div>
  )
}
