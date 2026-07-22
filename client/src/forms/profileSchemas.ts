import { z } from 'zod'

const optionalNumber = (min: number, max: number) => z.number().min(min).max(max).optional()

export const profileSchema = z.object({
  nickname: z.string().max(120).optional().or(z.literal('')),
  avatarUrl: z.string().url('Avatar must be a URL').optional().or(z.literal('')),
  bio: z.string().max(1000).optional().or(z.literal('')),
})

export const gameProfileSchema = z.object({
  gameId: z.string().uuid('Game ID must be a UUID'),
  inGameName: z.string().max(120).optional().or(z.literal('')),
  rankId: z.string().uuid().optional().or(z.literal('')),
  preferredRoleId: z.string().uuid().optional().or(z.literal('')),
  secondaryRoleId: z.string().uuid().optional().or(z.literal('')),
  playStyle: z.string().max(1000).optional().or(z.literal('')),
  shortDescription: z.string().max(1000).optional().or(z.literal('')),
  visibleOnRadar: z.boolean().optional(),
})

export const stationPreferenceSchema = z.object({
  deskHeightCm: optionalNumber(60, 120),
  chairAngleDegree: optionalNumber(90, 145),
  rgbColor: z
    .string()
    .regex(/^#[0-9A-Fa-f]{6}$/, 'Use #RRGGBB')
    .optional()
    .or(z.literal('')),
  brightness: optionalNumber(0, 100),
  mouseDpi: optionalNumber(200, 32000),
  nightMode: z.boolean().optional(),
})

export type ProfileFormValues = z.infer<typeof profileSchema>
export type GameProfileFormValues = z.infer<typeof gameProfileSchema>
export type StationPreferenceFormValues = z.infer<typeof stationPreferenceSchema>
