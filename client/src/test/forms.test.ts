import { describe, expect, it } from 'vitest'
import { loginSchema, registerSchema } from '../forms/authSchemas'

describe('auth form schemas', () => {
  it('accepts valid login values', () => {
    expect(
      loginSchema.safeParse({
        identifier: 'gamer@example.com',
        password: 'password1',
      }).success,
    ).toBe(true)
  })

  it('requires matching register passwords and contact information', () => {
    expect(
      registerSchema.safeParse({
        fullName: 'Nexus Gamer',
        password: 'password1',
        confirmPassword: 'password2',
      }).success,
    ).toBe(false)
  })
})
