import { useQuery } from '@tanstack/react-query'
import { mapApiError } from '../api/errors'
import { sessionApi } from '../api/sessionApi'
import { EmptyState, ErrorState, LoadingState } from '../components/ui'

export function SessionHistoryPage() {
  const sessionsQuery = useQuery({
    queryKey: ['sessions', 'history'],
    queryFn: sessionApi.history,
  })

  if (sessionsQuery.isLoading) {
    return <LoadingState label="Loading sessions" />
  }
  if (sessionsQuery.error) {
    return <ErrorState message={mapApiError(sessionsQuery.error)} />
  }
  if (!sessionsQuery.data?.length) {
    return <EmptyState title="No sessions" description="Session history will appear here." />
  }

  return (
    <article className="panel">
      <h2>Session history</h2>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Status</th>
              <th>Station</th>
              <th>Started</th>
              <th>Ended</th>
              <th>Cost</th>
            </tr>
          </thead>
          <tbody>
            {sessionsQuery.data.map((session) => (
              <tr key={session.id}>
                <td>{session.status}</td>
                <td>{session.stationId}</td>
                <td>{formatDate(session.startedAt)}</td>
                <td>{formatDate(session.endedAt)}</td>
                <td>{session.actualCost ?? session.estimatedCost ?? '-'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </article>
  )
}

function formatDate(value?: string | null) {
  return value ? new Date(value).toLocaleString() : '-'
}
