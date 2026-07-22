type EmptyStateProps = {
  title: string
  description?: string
}

export function EmptyState({ title, description }: EmptyStateProps) {
  return (
    <section className="state state-empty">
      <h1>{title}</h1>
      {description ? <p>{description}</p> : null}
    </section>
  )
}
