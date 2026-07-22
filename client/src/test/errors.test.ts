import { AxiosError, type AxiosResponse } from 'axios'
import { describe, expect, it } from 'vitest'
import { mapApiError } from '../api/errors'

describe('mapApiError', () => {
  it('uses validation message when present', () => {
    const error = new AxiosError(
      'Bad request',
      '400',
      undefined,
      undefined,
      response(400, {
        message: 'Validation failed',
        violations: [{ field: 'email', message: 'Email is invalid' }],
      }),
    )

    expect(mapApiError(error)).toBe('Email is invalid')
  })

  it('hides technical messages', () => {
    const error = new AxiosError(
      'Server error',
      '500',
      undefined,
      undefined,
      response(500, { message: 'java.lang.NullPointerException' }),
    )

    expect(mapApiError(error)).toBe(
      'The request could not be completed. Please check the form and try again.',
    )
  })
})

function response(status: number, data: unknown): AxiosResponse {
  return {
    data,
    status,
    statusText: String(status),
    headers: {},
    config: { headers: {} },
  } as AxiosResponse
}
