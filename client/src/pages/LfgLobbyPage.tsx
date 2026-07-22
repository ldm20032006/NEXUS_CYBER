import { useEffect, useMemo, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { mapApiError } from '../api/errors'
import { lfgApi } from '../api/lfgApi'
import { profileApi } from '../api/profileApi'
import { env } from '../app/env'
import { useAuthStore } from '../auth/authStore'
import { EmptyState, ErrorState, LoadingState, useToast } from '../components/ui'
import {
  canShowVoiceControls,
  classifyLfgEvent,
  createEventDeduper,
  isLobbyMessagePayload,
  isLobbyPayload,
  mergeMessages,
} from '../features/lfg/lfgRealtime'
import { webSocketClient } from '../websocket/webSocketClient'
import type {
  DomainEventEnvelope,
  GamerGameProfile,
  LfgSignal,
  Lobby,
  LobbyMessage,
  PublicGamerProfile,
  TeamInvitation,
} from '../types/api'

const pageSize = 6

function pageItems<T>(items: T[], page: number) {
  const start = page * pageSize
  return items.slice(start, start + pageSize)
}

function optionLabel(profile: GamerGameProfile) {
  return [profile.gameName, profile.rankName, profile.preferredRoleName].filter(Boolean).join(' / ')
}

export function LfgLobbyPage() {
  const queryClient = useQueryClient()
  const toast = useToast()
  const user = useAuthStore((state) => state.user)
  const [branchId, setBranchId] = useState(user?.branchId ?? '')
  const [gameProfileId, setGameProfileId] = useState('')
  const [zoneId, setZoneId] = useState('')
  const [targetMembers, setTargetMembers] = useState(5)
  const [message, setMessage] = useState('')
  const [inviteMessage, setInviteMessage] = useState('')
  const [selectedLobbyId, setSelectedLobbyId] = useState('')
  const [chatInput, setChatInput] = useState('')
  const [radarPage, setRadarPage] = useState(0)
  const [chatPage, setChatPage] = useState(0)
  const [realtimeMessages, setRealtimeMessages] = useState<LobbyMessage[]>([])
  const [voiceTokenPreview, setVoiceTokenPreview] = useState('')
  const [voiceExpiresAt, setVoiceExpiresAt] = useState('')
  const dedupeRef = useRef(createEventDeduper())

  const gameProfilesQuery = useQuery({
    queryKey: ['profiles', 'game-profiles'],
    queryFn: profileApi.gameProfiles,
  })
  const selectedGameProfile = gameProfilesQuery.data?.find((profile) => profile.id === gameProfileId)

  const radarQuery = useQuery({
    queryKey: ['lfg', 'radar', branchId],
    queryFn: () => lfgApi.radarUsers(branchId),
    enabled: Boolean(branchId),
    refetchInterval: 10000,
  })
  const signalsQuery = useQuery({
    queryKey: ['lfg', 'signals', branchId, selectedGameProfile?.gameId, selectedGameProfile?.rankId, selectedGameProfile?.preferredRoleId, zoneId],
    queryFn: () =>
      lfgApi.searchSignals({
        branchId,
        gameId: selectedGameProfile?.gameId,
        rankId: selectedGameProfile?.rankId ?? undefined,
        roleId: selectedGameProfile?.preferredRoleId ?? undefined,
        zoneId: zoneId || undefined,
      }),
    enabled: Boolean(branchId),
    refetchInterval: 10000,
  })
  const mySignalsQuery = useQuery({
    queryKey: ['lfg', 'signals', 'me'],
    queryFn: lfgApi.mySignals,
    refetchInterval: 15000,
  })
  const receivedInvitationsQuery = useQuery({
    queryKey: ['lfg', 'invitations', 'received'],
    queryFn: lfgApi.receivedInvitations,
    refetchInterval: 10000,
  })
  const sentInvitationsQuery = useQuery({
    queryKey: ['lfg', 'invitations', 'sent'],
    queryFn: lfgApi.sentInvitations,
    refetchInterval: 15000,
  })
  const lobbyQuery = useQuery({
    queryKey: ['lfg', 'lobby', selectedLobbyId],
    queryFn: () => lfgApi.getLobby(selectedLobbyId),
    enabled: Boolean(selectedLobbyId),
    refetchInterval: 10000,
  })
  const messagesQuery = useQuery({
    queryKey: ['lfg', 'lobby', selectedLobbyId, 'messages', chatPage],
    queryFn: () => lfgApi.messages(selectedLobbyId, chatPage, 20),
    enabled: Boolean(selectedLobbyId),
    refetchInterval: 10000,
  })

  const messages = useMemo(
    () =>
      mergeMessages(
        messagesQuery.data ?? [],
        realtimeMessages.filter((messageItem) => messageItem.lobbyId === selectedLobbyId),
      ),
    [messagesQuery.data, realtimeMessages, selectedLobbyId],
  )

  useEffect(() => {
    const subscription = webSocketClient.subscribe<DomainEventEnvelope<unknown>>(
      '/user/queue/notifications',
      (envelope) => {
        if (dedupeRef.current(envelope.eventId)) {
          return
        }
        const kind = classifyLfgEvent(envelope.eventType)
        if (kind === 'invitation') {
          void queryClient.invalidateQueries({ queryKey: ['lfg', 'invitations'] })
          toast.notify('Team invitation updated.', 'info')
        }
      },
    )
    return () => subscription.unsubscribe()
  }, [queryClient, toast])

  useEffect(() => {
    if (!selectedLobbyId) {
      return undefined
    }
    const subscription = webSocketClient.subscribe<DomainEventEnvelope<unknown>>(
      `/topic/lobbies/${selectedLobbyId}`,
      (envelope) => {
        if (dedupeRef.current(envelope.eventId)) {
          return
        }
        const kind = classifyLfgEvent(envelope.eventType)
        const payload = envelope.payload
        if (kind === 'chat' && isLobbyMessagePayload(payload)) {
          setRealtimeMessages((current) => mergeMessages(current, [payload]))
        }
        if (kind === 'lobby' && isLobbyPayload(payload)) {
          queryClient.setQueryData(['lfg', 'lobby', selectedLobbyId], payload)
        }
      },
    )
    return () => subscription.unsubscribe()
  }, [queryClient, selectedLobbyId])

  const createSignalMutation = useMutation({
    mutationFn: () => {
      if (!selectedGameProfile) {
        throw new Error('Select a game profile first')
      }
      return lfgApi.createSignal({
        gameId: selectedGameProfile.gameId,
        rankId: selectedGameProfile.rankId ?? undefined,
        roleId: selectedGameProfile.preferredRoleId ?? undefined,
        zoneId: zoneId || undefined,
        targetMembers,
        message: message || undefined,
      })
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['lfg', 'signals'] })
      toast.notify('LFG signal created.', 'success')
    },
    onError: (error) => toast.notify(mapApiError(error), 'error'),
  })
  const inviteMutation = useMutation({
    mutationFn: (receiverId: string) =>
      lfgApi.invite({
        receiverId,
        lobbyId: lobbyQuery.data?.id,
        message: inviteMessage || undefined,
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['lfg', 'invitations'] })
      toast.notify('Invitation sent.', 'success')
    },
    onError: (error) => toast.notify(mapApiError(error), 'error'),
  })
  const blockMutation = useMutation({
    mutationFn: (userId: string) => lfgApi.blockUser(userId, 'Blocked from Radar'),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['lfg', 'radar'] })
      toast.notify('User blocked.', 'success')
    },
    onError: (error) => toast.notify(mapApiError(error), 'error'),
  })
  const reportMutation = useMutation({
    mutationFn: (userId: string) =>
      lfgApi.reportUser(userId, { reason: 'Radar abuse', context: 'Reported from Gamer Radar UI' }),
    onSuccess: () => toast.notify('Report submitted.', 'success'),
    onError: (error) => toast.notify(mapApiError(error), 'error'),
  })
  const acceptMutation = useMutation({
    mutationFn: lfgApi.acceptInvitation,
    onSuccess: async (lobby) => {
      setSelectedLobbyId(lobby.id)
      await queryClient.invalidateQueries({ queryKey: ['lfg', 'invitations'] })
      toast.notify('Invitation accepted.', 'success')
    },
    onError: (error) => toast.notify(mapApiError(error), 'error'),
  })
  const rejectMutation = useMutation({
    mutationFn: lfgApi.rejectInvitation,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['lfg', 'invitations'] }),
    onError: (error) => toast.notify(mapApiError(error), 'error'),
  })
  const createLobbyMutation = useMutation({
    mutationFn: () => {
      if (!selectedGameProfile) {
        throw new Error('Select a game profile first')
      }
      return lfgApi.createLobby({ gameId: selectedGameProfile.gameId, name: message || undefined })
    },
    onSuccess: (lobby) => {
      setSelectedLobbyId(lobby.id)
      toast.notify('Lobby created.', 'success')
    },
    onError: (error) => toast.notify(mapApiError(error), 'error'),
  })
  const lobbyActionMutation = useMutation({
    mutationFn: async (action: { type: 'leave' | 'disband' | 'kick' | 'transfer'; userId?: string }) => {
      if (!selectedLobbyId) {
        throw new Error('Select a lobby first')
      }
      if (action.type === 'leave') {
        return lfgApi.leaveLobby(selectedLobbyId)
      }
      if (action.type === 'disband') {
        await lfgApi.disbandLobby(selectedLobbyId)
        return null
      }
      if (action.type === 'kick' && action.userId) {
        return lfgApi.kickMember(selectedLobbyId, action.userId)
      }
      if (action.type === 'transfer' && action.userId) {
        return lfgApi.transferLeader(selectedLobbyId, action.userId)
      }
      throw new Error('Invalid lobby action')
    },
    onSuccess: (lobby) => {
      if (lobby) {
        queryClient.setQueryData(['lfg', 'lobby', selectedLobbyId], lobby)
      } else {
        setSelectedLobbyId('')
      }
    },
    onError: (error) => toast.notify(mapApiError(error), 'error'),
  })
  const sendMessageMutation = useMutation({
    mutationFn: () => lfgApi.sendMessage(selectedLobbyId, { content: chatInput }),
    onSuccess: (created) => {
      setChatInput('')
      setRealtimeMessages((current) => mergeMessages(current, [created]))
    },
    onError: (error) => toast.notify(mapApiError(error), 'error'),
  })
  const voiceTokenMutation = useMutation({
    mutationFn: () => {
      if (!selectedLobbyId) {
        throw new Error('Select a lobby first')
      }
      return lfgApi.voiceToken(selectedLobbyId)
    },
    onSuccess: (response) => {
      if (response.status === 'VOICE_UNAVAILABLE' || !response.token) {
        setVoiceTokenPreview('')
        setVoiceExpiresAt('')
        toast.notify('Voice is unavailable. Text chat is still active.', 'error')
        void queryClient.invalidateQueries({ queryKey: ['lfg', 'lobby', selectedLobbyId] })
        return
      }
      setVoiceTokenPreview(`${response.token.slice(0, 10)}...`)
      setVoiceExpiresAt(response.expiresAt ?? '')
      toast.notify('Voice token issued.', 'success')
      void queryClient.invalidateQueries({ queryKey: ['lfg', 'lobby', selectedLobbyId] })
    },
    onError: (error) => toast.notify(mapApiError(error), 'error'),
  })

  const radarUsers = useMemo(() => radarQuery.data ?? [], [radarQuery.data])
  const signals = signalsQuery.data ?? []
  const received = receivedInvitationsQuery.data ?? []
  const sent = sentInvitationsQuery.data ?? []
  const lobby = lobbyQuery.data
  const isLeader = lobby?.leaderId === user?.id

  return (
    <div className="lfg-layout">
      <section className="panel lfg-hero">
        <div className="panel-heading">
          <div>
            <p className="eyebrow">Radar</p>
            <h2>Find teammates</h2>
          </div>
          <span className="status-pill status-muted">Realtime + polling fallback</span>
        </div>
        <div className="filter-grid">
          <label className="field">
            Branch ID
            <input value={branchId} onChange={(event) => setBranchId(event.target.value)} />
          </label>
          <label className="field">
            Game profile
            <select value={gameProfileId} onChange={(event) => setGameProfileId(event.target.value)}>
              <option value="">Select profile</option>
              {(gameProfilesQuery.data ?? []).map((profile) => (
                <option value={profile.id} key={profile.id}>
                  {optionLabel(profile)}
                </option>
              ))}
            </select>
          </label>
          <label className="field">
            Zone ID
            <input value={zoneId} onChange={(event) => setZoneId(event.target.value)} placeholder="Optional" />
          </label>
          <label className="field">
            Target members
            <input
              type="number"
              min={2}
              max={10}
              value={targetMembers}
              onChange={(event) => setTargetMembers(Number(event.target.value))}
            />
          </label>
        </div>
        <label className="field">
          Message
          <textarea value={message} onChange={(event) => setMessage(event.target.value)} />
        </label>
        <div className="action-row">
          <button
            type="button"
            className="button button-primary"
            onClick={() => createSignalMutation.mutate()}
            disabled={!selectedGameProfile || createSignalMutation.isPending}
          >
            Create LFG
          </button>
          <button
            type="button"
            className="button button-secondary"
            onClick={() => createLobbyMutation.mutate()}
            disabled={!selectedGameProfile || createLobbyMutation.isPending}
          >
            Create lobby
          </button>
        </div>
      </section>

      <section className="page-grid">
        <article className="panel">
          <h2>Gamer cards</h2>
          {radarQuery.isLoading ? <LoadingState label="Loading Radar" /> : null}
          {radarQuery.error ? <ErrorState message={mapApiError(radarQuery.error)} /> : null}
          {!radarQuery.isLoading && !radarQuery.error && radarUsers.length === 0 ? (
            <EmptyState title="No gamers found" description="Adjust filters or start an active session." />
          ) : null}
          <div className="card-grid">
            {pageItems(radarUsers, radarPage).map((gamer) => (
              <GamerCard
                gamer={gamer}
                key={gamer.userId}
                onInvite={() => inviteMutation.mutate(gamer.userId)}
                onBlock={() => blockMutation.mutate(gamer.userId)}
                onReport={() => reportMutation.mutate(gamer.userId)}
              />
            ))}
          </div>
          <Pager
            page={radarPage}
            total={radarUsers.length}
            onPrevious={() => setRadarPage((value) => Math.max(0, value - 1))}
            onNext={() => setRadarPage((value) => value + 1)}
          />
        </article>

        <article className="panel">
          <h2>LFG signals</h2>
          {signalsQuery.isLoading ? <LoadingState label="Loading LFG signals" /> : null}
          {signalsQuery.error ? <ErrorState message={mapApiError(signalsQuery.error)} /> : null}
          {signals.length === 0 && !signalsQuery.isLoading ? (
            <EmptyState title="No matching signals" description="Create one or change the Radar filters." />
          ) : null}
          <div className="list-stack">
            {signals.map((signal) => (
              <SignalRow signal={signal} key={signal.id} />
            ))}
          </div>
          <h3>My signals</h3>
          <div className="list-stack">
            {(mySignalsQuery.data ?? []).map((signal) => (
              <div className="compact-row" key={signal.id}>
                <SignalRow signal={signal} />
                <button
                  type="button"
                  className="button button-secondary"
                  onClick={() => lfgApi.renewSignal(signal.id).then(() => queryClient.invalidateQueries({ queryKey: ['lfg', 'signals'] }))}
                >
                  Renew
                </button>
              </div>
            ))}
          </div>
        </article>
      </section>

      <section className="page-grid">
        <article className="panel">
          <h2>Invitations</h2>
          <label className="field">
            Invite message
            <input value={inviteMessage} onChange={(event) => setInviteMessage(event.target.value)} />
          </label>
          <InvitationList
            title="Received"
            invitations={received}
            loading={receivedInvitationsQuery.isLoading}
            error={receivedInvitationsQuery.error}
            onAccept={(id) => acceptMutation.mutate(id)}
            onReject={(id) => rejectMutation.mutate(id)}
            onOpenLobby={(id) => setSelectedLobbyId(id)}
          />
          <InvitationList
            title="Sent"
            invitations={sent}
            loading={sentInvitationsQuery.isLoading}
            error={sentInvitationsQuery.error}
            onOpenLobby={(id) => setSelectedLobbyId(id)}
          />
        </article>

        <article className="panel">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">Lobby</p>
              <h2>Members and chat</h2>
            </div>
            {lobby ? <span className="status-pill status-success">{lobby.status}</span> : null}
          </div>
          <label className="field">
            Lobby ID
            <input value={selectedLobbyId} onChange={(event) => setSelectedLobbyId(event.target.value)} />
          </label>
          {lobbyQuery.isLoading ? <LoadingState label="Loading lobby" /> : null}
          {lobbyQuery.error ? <ErrorState message={mapApiError(lobbyQuery.error)} /> : null}
          {!lobby && !lobbyQuery.isLoading ? (
            <EmptyState title="No lobby selected" description="Accept an invitation or create a lobby." />
          ) : null}
          {lobby ? (
            <LobbyPanel
              lobby={lobby}
              isLeader={isLeader}
              currentUserId={user?.id}
              onLeave={() => lobbyActionMutation.mutate({ type: 'leave' })}
              onDisband={() => lobbyActionMutation.mutate({ type: 'disband' })}
              onKick={(userId) => lobbyActionMutation.mutate({ type: 'kick', userId })}
              onTransfer={(userId) => lobbyActionMutation.mutate({ type: 'transfer', userId })}
            />
          ) : null}
          {canShowVoiceControls(env.VITE_ENABLE_VOICE, lobby) ? (
            <section className="voice-panel">
              <div>
                <h3>Voice channel</h3>
                <p>
                  {lobby?.voiceStatus ?? 'DISABLED'} / {lobby?.voiceProvider ?? 'mock'}
                </p>
              </div>
              <div className="action-row">
                <button
                  type="button"
                  className="button button-secondary"
                  onClick={() => voiceTokenMutation.mutate()}
                  disabled={voiceTokenMutation.isPending}
                >
                  Get voice token
                </button>
              </div>
              {voiceTokenPreview ? (
                <p className="muted-text">
                  Token {voiceTokenPreview} expires {voiceExpiresAt ? new Date(voiceExpiresAt).toLocaleTimeString() : 'soon'}
                </p>
              ) : null}
            </section>
          ) : null}
          {lobby ? (
            <section className="chat-panel">
              <div className="chat-history">
                {messages.length === 0 && !messagesQuery.isLoading ? (
                  <EmptyState title="No messages" description="Start the lobby chat." />
                ) : null}
                {messages.map((item) => (
                  <div className="chat-message" key={item.id}>
                    <strong>{item.senderId === user?.id ? 'You' : item.senderId}</strong>
                    <p>{item.content}</p>
                    <span>{new Date(item.sentAt).toLocaleTimeString()}</span>
                  </div>
                ))}
              </div>
              <div className="action-row">
                <button type="button" className="button button-secondary" onClick={() => setChatPage((value) => value + 1)}>
                  Load older
                </button>
              </div>
              <form
                className="chat-form"
                onSubmit={(event) => {
                  event.preventDefault()
                  if (chatInput.trim()) {
                    sendMessageMutation.mutate()
                  }
                }}
              >
                <input value={chatInput} onChange={(event) => setChatInput(event.target.value)} maxLength={1000} />
                <button type="submit" className="button button-primary" disabled={sendMessageMutation.isPending}>
                  Send
                </button>
              </form>
            </section>
          ) : null}
        </article>
      </section>
    </div>
  )
}

function GamerCard({
  gamer,
  onInvite,
  onBlock,
  onReport,
}: {
  gamer: PublicGamerProfile
  onInvite: () => void
  onBlock: () => void
  onReport: () => void
}) {
  return (
    <article className="gamer-card">
      <div className="avatar">{(gamer.nickname ?? 'G').slice(0, 1).toUpperCase()}</div>
      <div>
        <h3>{gamer.nickname ?? 'Gamer'}</h3>
        <p>{gamer.bio ?? 'Ready to team up.'}</p>
      </div>
      <div className="action-row">
        <button type="button" className="button button-primary" onClick={onInvite}>
          Invite
        </button>
        <button type="button" className="button button-secondary" onClick={onBlock}>
          Block
        </button>
        <button type="button" className="button button-secondary" onClick={onReport}>
          Report
        </button>
      </div>
    </article>
  )
}

function SignalRow({ signal }: { signal: LfgSignal }) {
  return (
    <div className="compact-row">
      <span className="status-pill status-muted">{signal.status}</span>
      <span>{signal.targetMembers} members</span>
      <span>{signal.message ?? 'No message'}</span>
      <span>{new Date(signal.expiresAt).toLocaleTimeString()}</span>
    </div>
  )
}

function InvitationList({
  title,
  invitations,
  loading,
  error,
  onAccept,
  onReject,
  onOpenLobby,
}: {
  title: string
  invitations: TeamInvitation[]
  loading: boolean
  error: unknown
  onAccept?: (id: string) => void
  onReject?: (id: string) => void
  onOpenLobby: (id: string) => void
}) {
  return (
    <section className="list-stack">
      <h3>{title}</h3>
      {loading ? <LoadingState label={`Loading ${title.toLowerCase()} invitations`} /> : null}
      {error ? <ErrorState message={mapApiError(error)} /> : null}
      {!loading && !error && invitations.length === 0 ? (
        <EmptyState title="No invitations" description="Realtime updates will appear here." />
      ) : null}
      {invitations.map((invitation) => (
        <div className="invitation-row" key={invitation.id}>
          <span className="status-pill status-muted">{invitation.status}</span>
          <span>{invitation.message ?? 'No message'}</span>
          <span>{new Date(invitation.expiresAt).toLocaleTimeString()}</span>
          <div className="action-row">
            {onAccept && invitation.status === 'PENDING' ? (
              <button type="button" className="button button-primary" onClick={() => onAccept(invitation.id)}>
                Accept
              </button>
            ) : null}
            {onReject && invitation.status === 'PENDING' ? (
              <button type="button" className="button button-secondary" onClick={() => onReject(invitation.id)}>
                Reject
              </button>
            ) : null}
            {invitation.lobbyId ? (
              <button type="button" className="button button-secondary" onClick={() => onOpenLobby(invitation.lobbyId ?? '')}>
                Open lobby
              </button>
            ) : null}
          </div>
        </div>
      ))}
    </section>
  )
}

function LobbyPanel({
  lobby,
  isLeader,
  currentUserId,
  onLeave,
  onDisband,
  onKick,
  onTransfer,
}: {
  lobby: Lobby
  isLeader: boolean
  currentUserId?: string
  onLeave: () => void
  onDisband: () => void
  onKick: (userId: string) => void
  onTransfer: (userId: string) => void
}) {
  return (
    <section className="list-stack">
      <dl className="details">
        <dt>Name</dt>
        <dd>{lobby.name ?? '-'}</dd>
        <dt>Capacity</dt>
        <dd>
          {lobby.members.length}/{lobby.maxMembers}
        </dd>
      </dl>
      <div className="member-list">
        {lobby.members.map((member) => (
          <div className="member-row" key={member.userId}>
            <span>{member.userId === currentUserId ? 'You' : member.userId}</span>
            <span>{member.role}</span>
            <span>{member.status}</span>
            {isLeader && member.userId !== currentUserId ? (
              <div className="action-row">
                <button type="button" className="button button-secondary" onClick={() => onTransfer(member.userId)}>
                  Leader
                </button>
                <button type="button" className="button button-danger" onClick={() => onKick(member.userId)}>
                  Kick
                </button>
              </div>
            ) : null}
          </div>
        ))}
      </div>
      <div className="action-row">
        <button type="button" className="button button-secondary" onClick={onLeave}>
          Leave
        </button>
        {isLeader ? (
          <button type="button" className="button button-danger" onClick={onDisband}>
            Disband
          </button>
        ) : null}
      </div>
    </section>
  )
}

function Pager({
  page,
  total,
  onPrevious,
  onNext,
}: {
  page: number
  total: number
  onPrevious: () => void
  onNext: () => void
}) {
  const hasNext = (page + 1) * pageSize < total
  return (
    <div className="pager">
      <button type="button" className="button button-secondary" onClick={onPrevious} disabled={page === 0}>
        Previous
      </button>
      <span>
        Page {page + 1} / {Math.max(1, Math.ceil(total / pageSize))}
      </span>
      <button type="button" className="button button-secondary" onClick={onNext} disabled={!hasNext}>
        Next
      </button>
    </div>
  )
}
