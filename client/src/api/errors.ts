import { AxiosError } from 'axios'

type ErrorBody = {
  message?: string
  error?: string
  violations?: Array<{ field: string; message: string }>
}

const genericMessage = 'The request could not be completed. Please check the form and try again.'

export function mapApiError(error: unknown): string {
  if (error instanceof AxiosError) {
    const body = error.response?.data as ErrorBody | undefined
    const firstViolation = body?.violations?.[0]
    if (firstViolation?.message) {
      return firstViolation.message
    }
    if (body?.message && !looksTechnical(body.message)) {
      return body.message
    }
    if (error.response?.status === 401) {
      return 'Please sign in again.'
    }
    if (error.response?.status === 403) {
      return 'You do not have access to this action.'
    }
    return genericMessage
  }
  return genericMessage
}

function looksTechnical(message: string) {
  return /exception|stack|trace|sql|hibernate|java\.|nullpointer/i.test(message)
}
