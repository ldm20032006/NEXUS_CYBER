import { postData } from './apiClient'
import type { StationHeartbeat } from '../types/api'

export const stationApi = {
  heartbeat: (stationId: string, stationSecret: string) =>
    postData<StationHeartbeat>(`/stations/${stationId}/heartbeat`, undefined, {
      'X-Station-Secret': stationSecret,
    }),
}
