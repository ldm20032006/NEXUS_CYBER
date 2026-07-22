import { EmptyState } from '../components/ui'

export function WorkspacePage({ title }: { title: string }) {
  return (
    <EmptyState
      title={title}
      description="The client foundation is ready for this workflow integration."
    />
  )
}
