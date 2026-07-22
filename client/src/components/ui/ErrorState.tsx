type ErrorStateProps = {
  title?: string
  message: string
}

export function ErrorState({ title = 'Request failed', message }: ErrorStateProps) {
  return (
    <section className="state state-error" role="alert">
      <h1>{title}</h1>
      <p>{message}</p>
    </section>
  )
}
