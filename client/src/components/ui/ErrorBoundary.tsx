import { Component, type ErrorInfo, type ReactNode } from 'react'
import { EmptyState } from './EmptyState'

type Props = {
  children: ReactNode
}

type State = {
  hasError: boolean
}

export class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false }

  static getDerivedStateFromError(): State {
    return { hasError: true }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('Unhandled UI error', { error: error.message, info })
  }

  render() {
    if (this.state.hasError) {
      return (
        <EmptyState
          title="Something went wrong"
          description="Reload the page or return to a safe section."
        />
      )
    }
    return this.props.children
  }
}
