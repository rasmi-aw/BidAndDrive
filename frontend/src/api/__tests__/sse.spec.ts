import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, setTokenProvider } from '@/api/http'
import { SseParser, openEventStream } from '@/api/sse'

const fetchMock = vi.fn<typeof fetch>()
const encoder = new TextEncoder()

function streamResponse(...chunks: string[]): Response {
  return new Response(new ReadableStream({
    start(controller) {
      chunks.forEach((chunk) => controller.enqueue(encoder.encode(chunk)))
      controller.close()
    },
  }), { headers: { 'Content-Type': 'text/event-stream' } })
}

describe('SseParser', () => {
  it('parses events split across chunks, ignoring comments', () => {
    const parser = new SseParser()

    expect(parser.push('event:connected\ndata:{"auctionId":"a1"}\n\n:heartbeat\n\nid:b1\nevent:bi')).toEqual([
      { event: 'connected', data: '{"auctionId":"a1"}', id: undefined },
    ])
    expect(parser.push('d\ndata: {"amount":1200}\n\n')).toEqual([
      { event: 'bid', data: '{"amount":1200}', id: 'b1' },
    ])
  })

  it('joins multi-line data, accepts CRLF and defaults the event name to message', () => {
    expect(new SseParser().push('data:line 1\r\ndata:line 2\r\n\r\n')).toEqual([
      { event: 'message', data: 'line 1\nline 2', id: undefined },
    ])
  })
})

describe('openEventStream', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', fetchMock)
    setTokenProvider(async () => 'token-123')
  })

  afterEach(() => {
    fetchMock.mockReset()
    vi.unstubAllGlobals()
  })

  it('reads the stream with the bearer token and hands each event over', async () => {
    fetchMock.mockResolvedValueOnce(streamResponse('event:connected\ndata:{}\n\n', 'event:bid\ndata:{"amount":1200}\n\n'))
    const events: string[] = []

    await openEventStream('/api/auctions/a1/stream', { onEvent: (e) => events.push(`${e.event}:${e.data}`) })

    const headers = new Headers(fetchMock.mock.calls[0][1]?.headers)
    expect(headers.get('Authorization')).toBe('Bearer token-123')
    expect(headers.get('Accept')).toContain('text/event-stream')
    expect(events).toEqual(['connected:{}', 'bid:{"amount":1200}'])
  })

  it('fails with the backend problem when the stream cannot be opened', async () => {
    fetchMock.mockResolvedValueOnce(new Response(JSON.stringify({ title: 'Resource not found', status: 404 }), {
      status: 404, headers: { 'Content-Type': 'application/problem+json' },
    }))

    const error = await openEventStream('/api/auctions/x/stream', { onEvent: () => {} }).catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).status).toBe(404)
  })
})
