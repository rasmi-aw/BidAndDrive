import { errorFrom, requestHeaders } from './http'

// Server-Sent Events over fetch(): unlike EventSource, fetch can send the Authorization header.

export interface SseMessage {
  event: string
  data: string
  id?: string
}

/** Incremental text/event-stream parser: feed it chunks, get complete events back. */
export class SseParser {
  private buffer = ''

  push(chunk: string): SseMessage[] {
    this.buffer += chunk.replace(/\r\n?/g, '\n')
    const messages: SseMessage[] = []
    let end: number
    while ((end = this.buffer.indexOf('\n\n')) >= 0) {
      const block = this.buffer.slice(0, end)
      this.buffer = this.buffer.slice(end + 2)
      const message = parseBlock(block)
      if (message) messages.push(message)
    }
    return messages
  }
}

function parseBlock(block: string): SseMessage | undefined {
  let event = 'message'
  let id: string | undefined
  const data: string[] = []
  for (const line of block.split('\n')) {
    if (!line || line.startsWith(':')) continue // comment / heartbeat
    const colon = line.indexOf(':')
    const field = colon < 0 ? line : line.slice(0, colon)
    const value = colon < 0 ? '' : line.slice(colon + 1).replace(/^ /, '')
    if (field === 'event') event = value
    else if (field === 'data') data.push(value)
    else if (field === 'id') id = value
  }
  return data.length ? { event, data: data.join('\n'), id } : undefined
}

export interface StreamOptions {
  onEvent: (message: SseMessage) => void
  signal?: AbortSignal
}

/**
 * Reads an SSE stream until the server ends it. Rejects with an ApiError if the stream cannot be opened,
 * and with the fetch error if the connection breaks (an AbortError when aborted through the signal).
 */
export async function openEventStream(url: string, options: StreamOptions): Promise<void> {
  const response = await fetch(url, {
    headers: await requestHeaders('text/event-stream, application/problem+json'),
    signal: options.signal,
  })
  if (!response.ok || !response.body) {
    throw await errorFrom(response)
  }
  const reader = response.body.pipeThrough(new TextDecoderStream()).getReader()
  const parser = new SseParser()
  for (;;) {
    const { value, done } = await reader.read()
    if (done) return
    parser.push(value).forEach(options.onEvent)
  }
}
