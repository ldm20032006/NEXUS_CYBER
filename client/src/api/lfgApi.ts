import { deleteData, getData, patchData, postData, putData } from './apiClient'
import type {
  CreateLobbyRequest,
  LfgSearchParams,
  LfgSignal,
  LfgSignalRequest,
  Lobby,
  LobbyMessage,
  LobbyMessageRequest,
  PublicGamerProfile,
  ReportUserRequest,
  TeamInvitation,
  TeamInvitationRequest,
  UserBlock,
  VoiceTokenResponse,
} from '../types/api'

export const lfgApi = {
  searchSignals: (params: LfgSearchParams) => getData<LfgSignal[]>('/lfg/signals', params),
  mySignals: () => getData<LfgSignal[]>('/lfg/signals/me'),
  createSignal: (body: LfgSignalRequest) =>
    postData<LfgSignal, LfgSignalRequest>('/lfg/signals', body),
  updateSignal: (id: string, body: LfgSignalRequest) =>
    putData<LfgSignal, LfgSignalRequest>(`/lfg/signals/${id}`, body),
  cancelSignal: (id: string) => deleteData<void>(`/lfg/signals/${id}`),
  renewSignal: (id: string) => postData<LfgSignal>(`/lfg/signals/${id}/renew`),

  radarUsers: (branchId: string) =>
    getData<PublicGamerProfile[]>('/social/radar/users', { branchId }),
  blockUser: (userId: string, reason?: string) =>
    postData<UserBlock, { reason?: string }>(`/social/blocks/${userId}`, { reason }),
  reportUser: (userId: string, body: ReportUserRequest) =>
    postData<void, ReportUserRequest>(`/social/reports/${userId}`, body),

  invite: (body: TeamInvitationRequest) =>
    postData<TeamInvitation, TeamInvitationRequest>('/team-invitations', body),
  receivedInvitations: () => getData<TeamInvitation[]>('/team-invitations/received'),
  sentInvitations: () => getData<TeamInvitation[]>('/team-invitations/sent'),
  acceptInvitation: (id: string) => patchData<Lobby>(`/team-invitations/${id}/accept`),
  rejectInvitation: (id: string) => patchData<TeamInvitation>(`/team-invitations/${id}/reject`),
  cancelInvitation: (id: string) => patchData<TeamInvitation>(`/team-invitations/${id}/cancel`),

  createLobby: (body: CreateLobbyRequest) =>
    postData<Lobby, CreateLobbyRequest>('/lobbies', body),
  getLobby: (id: string) => getData<Lobby>(`/lobbies/${id}`),
  leaveLobby: (id: string) => deleteData<Lobby>(`/lobbies/${id}/members/me`),
  kickMember: (id: string, userId: string) => deleteData<Lobby>(`/lobbies/${id}/members/${userId}`),
  transferLeader: (id: string, userId: string) => postData<Lobby>(`/lobbies/${id}/leader/${userId}`),
  disbandLobby: (id: string) => deleteData<void>(`/lobbies/${id}`),
  messages: (id: string, page = 0, size = 20) =>
    getData<LobbyMessage[]>(`/lobbies/${id}/messages`, { page, size }),
  sendMessage: (id: string, body: LobbyMessageRequest) =>
    postData<LobbyMessage, LobbyMessageRequest>(`/lobbies/${id}/messages`, body),
  voiceToken: (id: string) => postData<VoiceTokenResponse>(`/lobbies/${id}/voice/token`),
}
