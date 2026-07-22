import { z } from 'zod'

export const loginSchema = z.object({
  identifier: z.string().min(3, 'Email or phone is required'),
  password: z.string().min(8, 'Password must be at least 8 characters'),
})

export const registerSchema = z
  .object({
    fullName: z.string().min(2, 'Full name is required'),
    displayName: z.string().max(120).optional().or(z.literal('')),
    email: z.string().email('Valid email is required'),
    phone: z.string().min(8).optional().or(z.literal('')),
    password: z.string().min(8, 'Password must be at least 8 characters'),
    confirmPassword: z.string().min(8),
  })
  .refine((value) => value.password === value.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
  })

export const forgotPasswordSchema = z.object({
  identifier: z.string().min(3, 'Email or phone is required'),
})

export const resetPasswordSchema = z.object({
  token: z.string().min(12, 'Reset token is required'),
  newPassword: z.string().min(8, 'Password must be at least 8 characters'),
})

export const changePasswordSchema = z.object({
  currentPassword: z.string().min(8, 'Current password is required'),
  newPassword: z.string().min(8, 'Password must be at least 8 characters'),
})

export type LoginFormValues = z.infer<typeof loginSchema>
export type RegisterFormValues = z.infer<typeof registerSchema>
export type ForgotPasswordFormValues = z.infer<typeof forgotPasswordSchema>
export type ResetPasswordFormValues = z.infer<typeof resetPasswordSchema>
export type ChangePasswordFormValues = z.infer<typeof changePasswordSchema>
