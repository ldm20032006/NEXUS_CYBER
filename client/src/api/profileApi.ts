import { deleteData, getData, postData, putData } from './apiClient'
import type {
  GamerGameProfile,
  GamerGameProfileRequest,
  GamerProfileRequest,
  PublicGamerProfile,
  StationPreference,
  StationPreferenceRequest,
} from '../types/api'

export const profileApi = {
  me: () => getData<PublicGamerProfile>('/profiles/me'),
  updateMe: (body: GamerProfileRequest) =>
    putData<PublicGamerProfile, GamerProfileRequest>('/profiles/me', body),
  gameProfiles: () => getData<GamerGameProfile[]>('/profiles/me/game-profiles'),
  createGameProfile: (body: GamerGameProfileRequest) =>
    postData<GamerGameProfile, GamerGameProfileRequest>(
      '/profiles/me/game-profiles',
      body,
    ),
  updateGameProfile: (id: string, body: GamerGameProfileRequest) =>
    putData<GamerGameProfile, GamerGameProfileRequest>(
      `/profiles/me/game-profiles/${id}`,
      body,
    ),
  deleteGameProfile: (id: string) =>
    deleteData<void>(`/profiles/me/game-profiles/${id}`),
  stationPreference: () => getData<StationPreference>('/profiles/me/station-preference'),
  updateStationPreference: (body: StationPreferenceRequest) =>
    putData<StationPreference, StationPreferenceRequest>(
      '/profiles/me/station-preference',
      body,
    ),
}
