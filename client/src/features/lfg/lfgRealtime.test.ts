import { describe, expect, it } from 'vitest'
import {
  classifyLfgEvent,
  canShowVoiceControls,
  createEventDeduper,
  isLobbyMessagePayload,
  isLobbyPayload,
  mergeMessages,
} from './lfgRealtime'
import type { Lobby, LobbyMessage } from '../../types/api'

describe('lfgRealtime', () => {
  it('classifies invitation, lobby, and chat events', () => {
    expect(classifyLfgEvent('TEAM_INVITATION')).toBe('invitation')
    expect(classifyLfgEvent('LOBBY_UPDATED')).toBe('lobby')
    expect(classifyLfgEvent('LOBBY_MESSAGE')).toBe('chat')
    expect(classifyLfgEvent('ORDER_CREATED')).toBe('unknown')
  })

  it('deduplicates reconnect replayed events by eventId', () => {
    const wasSeen = createEventDeduper()

    expect(wasSeen('event-1')).toBe(false)
    expect(wasSeen('event-1')).toBe(true)
    expect(wasSeen('event-2')).toBe(false)
  })

  it('merges chat history and realtime messages without duplicates', () => {
    const first: LobbyMessage = {
      id: 'message-1',
      lobbyId: 'lobby-1',
      senderId: 'user-1',
      messageType: 'TEXT',
      content: 'hello',
      sentAt: '2026-07-21T00:00:01Z',
    }
    const duplicate: LobbyMessage = { ...first, content: 'hello again' }
    const second: LobbyMessage = {
      id: 'message-2',
      lobbyId: 'lobby-1',
      senderId: 'user-2',
      messageType: 'TEXT',
      content: 'ready',
      sentAt: '2026-07-21T00:00:02Z',
    }

    expect(mergeMessages([first], [duplicate, second])).toEqual([duplicate, second])
  })

  it('guards lobby and chat payloads', () => {
    const lobby: Lobby = {
      id: 'lobby-1',
      leaderId: 'user-1',
      branchId: 'branch-1',
      gameId: 'game-1',
      maxMembers: 5,
      status: 'OPEN',
      members: [],
    }
    const message: LobbyMessage = {
      id: 'message-1',
      lobbyId: 'lobby-1',
      senderId: 'user-1',
      messageType: 'TEXT',
      content: 'hello',
      sentAt: '2026-07-21T00:00:01Z',
    }

    expect(isLobbyPayload(lobby)).toBe(true)
    expect(isLobbyMessagePayload(message)).toBe(true)
    expect(isLobbyPayload(message)).toBe(false)
  })

  it('shows voice controls only when feature flag is enabled and lobby is open', () => {
    const lobby: Lobby = {
      id: 'lobby-1',
      leaderId: 'user-1',
      branchId: 'branch-1',
      gameId: 'game-1',
      maxMembers: 5,
      status: 'OPEN',
      voiceStatus: 'DISABLED',
      members: [],
    }

    expect(canShowVoiceControls(false, lobby)).toBe(false)
    expect(canShowVoiceControls(true, null)).toBe(false)
    expect(canShowVoiceControls(true, { ...lobby, status: 'CLOSED' })).toBe(false)
    expect(canShowVoiceControls(true, lobby)).toBe(true)
  })
})
