import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import { useForm } from 'react-hook-form'
import { useLocation, useNavigate } from 'react-router-dom'
import { authApi } from '../api/authApi'
import { mapApiError } from '../api/errors'
import { useAuthStore } from '../auth/authStore'
import { defaultRouteForUser } from '../auth/roleNavigation'
import { ErrorState, useToast } from '../components/ui'
import {
  forgotPasswordSchema,
  loginSchema,
  registerSchema,
  resetPasswordSchema,
  type ForgotPasswordFormValues,
  type LoginFormValues,
  type RegisterFormValues,
  type ResetPasswordFormValues,
} from '../forms/authSchemas'

type AuthMode = 'login' | 'register' | 'forgot' | 'reset'

type LocationState = {
  from?: { pathname?: string }
}

export function AuthPage() {
  const [mode, setMode] = useState<AuthMode>('login')
  const title = useMemo(
    () =>
      ({
        login: 'Sign in',
        register: 'Register gamer',
        forgot: 'Forgot password',
        reset: 'Reset password',
      })[mode],
    [mode],
  )

  return (
    <section className="auth-stack">
      <div>
        <p className="eyebrow">Account</p>
        <h1>{title}</h1>
      </div>
      <div className="segmented" role="tablist" aria-label="Authentication mode">
        {(['login', 'register', 'forgot', 'reset'] as const).map((item) => (
          <button
            key={item}
            type="button"
            className={mode === item ? 'active' : ''}
            onClick={() => setMode(item)}
          >
            {item}
          </button>
        ))}
      </div>
      {mode === 'login' ? <LoginForm /> : null}
      {mode === 'register' ? <RegisterForm /> : null}
      {mode === 'forgot' ? <ForgotPasswordForm /> : null}
      {mode === 'reset' ? <ResetPasswordForm /> : null}
    </section>
  )
}

function LoginForm() {
  const navigate = useNavigate()
  const location = useLocation()
  const setAuth = useAuthStore((state) => state.setAuth)
  const { notify } = useToast()
  const [error, setError] = useState<string | null>(null)
  const form = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { identifier: '', password: '' },
  })

  const loginMutation = useMutation({
    mutationFn: authApi.login,
    onSuccess: (auth) => {
      setError(null)
      setAuth(auth)
      notify('Signed in', 'success')
      const state = location.state as LocationState | null
      navigate(state?.from?.pathname ?? defaultRouteForUser(auth.user), { replace: true })
    },
    onError: (apiError) => setError(mapApiError(apiError)),
  })

  return (
    <form className="form" onSubmit={form.handleSubmit((values) => loginMutation.mutate(values))}>
      {error ? <ErrorState message={error} /> : null}
      <label>
        Email or phone
        <input autoComplete="username" {...form.register('identifier')} />
        <span>{form.formState.errors.identifier?.message}</span>
      </label>
      <label>
        Password
        <input type="password" autoComplete="current-password" {...form.register('password')} />
        <span>{form.formState.errors.password?.message}</span>
      </label>
      <button className="button button-primary" type="submit" disabled={loginMutation.isPending}>
        {loginMutation.isPending ? 'Signing in' : 'Sign in'}
      </button>
    </form>
  )
}

function RegisterForm() {
  const navigate = useNavigate()
  const setAuth = useAuthStore((state) => state.setAuth)
  const { notify } = useToast()
  const [error, setError] = useState<string | null>(null)
  const form = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      fullName: '',
      displayName: '',
      email: '',
      phone: '',
      password: '',
      confirmPassword: '',
    },
  })
  const registerMutation = useMutation({
    mutationFn: (values: RegisterFormValues) =>
      authApi.register({
        fullName: values.fullName,
        displayName: values.displayName || undefined,
        email: values.email,
        phone: values.phone || undefined,
        password: values.password,
      }),
    onSuccess: (auth) => {
      setError(null)
      setAuth(auth)
      notify('Account created', 'success')
      navigate(defaultRouteForUser(auth.user), { replace: true })
    },
    onError: (apiError) => setError(mapApiError(apiError)),
  })

  return (
    <form className="form" onSubmit={form.handleSubmit((values) => registerMutation.mutate(values))}>
      {error ? <ErrorState message={error} /> : null}
      <label>
        Full name
        <input autoComplete="name" {...form.register('fullName')} />
        <span>{form.formState.errors.fullName?.message}</span>
      </label>
      <label>
        Display name
        <input {...form.register('displayName')} />
        <span>{form.formState.errors.displayName?.message}</span>
      </label>
      <label>
        Email
        <input autoComplete="email" {...form.register('email')} />
        <span>{form.formState.errors.email?.message}</span>
      </label>
      <label>
        Phone
        <input autoComplete="tel" {...form.register('phone')} />
        <span>{form.formState.errors.phone?.message}</span>
      </label>
      <label>
        Password
        <input type="password" autoComplete="new-password" {...form.register('password')} />
        <span>{form.formState.errors.password?.message}</span>
      </label>
      <label>
        Confirm password
        <input type="password" autoComplete="new-password" {...form.register('confirmPassword')} />
        <span>{form.formState.errors.confirmPassword?.message}</span>
      </label>
      <button className="button button-primary" type="submit" disabled={registerMutation.isPending}>
        {registerMutation.isPending ? 'Creating account' : 'Create account'}
      </button>
    </form>
  )
}

function ForgotPasswordForm() {
  const { notify } = useToast()
  const [error, setError] = useState<string | null>(null)
  const form = useForm<ForgotPasswordFormValues>({
    resolver: zodResolver(forgotPasswordSchema),
    defaultValues: { identifier: '' },
  })
  const mutation = useMutation({
    mutationFn: authApi.forgotPassword,
    onSuccess: () => {
      setError(null)
      notify('Reset instructions requested', 'success')
    },
    onError: (apiError) => setError(mapApiError(apiError)),
  })

  return (
    <form className="form" onSubmit={form.handleSubmit((values) => mutation.mutate(values))}>
      {error ? <ErrorState message={error} /> : null}
      <label>
        Email or phone
        <input autoComplete="username" {...form.register('identifier')} />
        <span>{form.formState.errors.identifier?.message}</span>
      </label>
      <button className="button button-primary" type="submit" disabled={mutation.isPending}>
        Request reset
      </button>
    </form>
  )
}

function ResetPasswordForm() {
  const { notify } = useToast()
  const [error, setError] = useState<string | null>(null)
  const form = useForm<ResetPasswordFormValues>({
    resolver: zodResolver(resetPasswordSchema),
    defaultValues: { token: '', newPassword: '' },
  })
  const mutation = useMutation({
    mutationFn: authApi.resetPassword,
    onSuccess: () => {
      setError(null)
      notify('Password reset', 'success')
      form.reset()
    },
    onError: (apiError) => setError(mapApiError(apiError)),
  })

  return (
    <form className="form" onSubmit={form.handleSubmit((values) => mutation.mutate(values))}>
      {error ? <ErrorState message={error} /> : null}
      <label>
        Reset token
        <input autoComplete="one-time-code" {...form.register('token')} />
        <span>{form.formState.errors.token?.message}</span>
      </label>
      <label>
        New password
        <input type="password" autoComplete="new-password" {...form.register('newPassword')} />
        <span>{form.formState.errors.newPassword?.message}</span>
      </label>
      <button className="button button-primary" type="submit" disabled={mutation.isPending}>
        Reset password
      </button>
    </form>
  )
}
