import { z } from 'zod'

const absoluteOrRootRelativeUrl = z.string().min(1).refine(
  (value) => {
    if (value.startsWith('/')) {
      return true
    }
    return z.string().url().safeParse(value).success
  },
  { message: 'Must be an absolute URL or a root-relative path' },
)

const envSchema = z.object({
  VITE_API_BASE_URL: absoluteOrRootRelativeUrl.default('http://localhost:8080/api/v1'),
  VITE_WS_URL: absoluteOrRootRelativeUrl.default('http://localhost:8080/ws'),
  VITE_APP_NAME: z.string().min(1).default('NEXUS Smart Cyber Esports'),
  VITE_ENABLE_PWA: z
    .string()
    .default('true')
    .transform((value) => value !== 'false'),
  VITE_ENABLE_VOICE: z
    .string()
    .default('false')
    .transform((value) => value === 'true'),
  VITE_PUSH_PUBLIC_KEY: z.string().optional(),
})

export const env = envSchema.parse({
  VITE_API_BASE_URL: import.meta.env.VITE_API_BASE_URL,
  VITE_WS_URL: import.meta.env.VITE_WS_URL ?? import.meta.env.VITE_WS_BASE_URL,
  VITE_APP_NAME: import.meta.env.VITE_APP_NAME,
  VITE_ENABLE_PWA: import.meta.env.VITE_ENABLE_PWA,
  VITE_ENABLE_VOICE: import.meta.env.VITE_ENABLE_VOICE,
  VITE_PUSH_PUBLIC_KEY: import.meta.env.VITE_PUSH_PUBLIC_KEY,
})
