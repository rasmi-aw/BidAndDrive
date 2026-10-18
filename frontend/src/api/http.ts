import type { ProblemDetail } from './types'

const PROBLEM_TYPE_PREFIX = 'urn:bidanddrive:problem:'

/** Any failed API call: carries the backend's ProblemDetail (or one built from the HTTP status). */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly problem: ProblemDetail,
  ) {
    super(problem.detail ?? problem.title ?? `Request failed with status ${status}`)
    this.name = 'ApiError'
  }

  /** Validation errors by field name, for showing next to form inputs. */
  get fieldErrors(): Record<string, string> {
    return Object.fromEntries((this.problem.errors ?? []).map((e) => [e.field, e.message]))
  }

  /** e.g. isType('bid-too-low') for urn:bidanddrive:problem:bid-too-low */
  isType(slug: string): boolean {
    return this.problem.type === PROBLEM_TYPE_PREFIX + slug
  }
}

export function toApiError(error: unknown): ApiError {
  if (error instanceof ApiError) {
    return error
  }
  return new ApiError(0, { title: 'Unexpected error', detail: error instanceof Error ? error.message : String(error) })
}

type TokenProvider = () => Promise<string | undefined>

let tokenProvider: TokenProvider = async () => undefined

/** Set once at startup by the auth store, so this module stays independent of keycloak-js. */
export function setTokenProvider(provider: TokenProvider): void {
  tokenProvider = provider
}

export interface RequestOptions {
  method?: 'GET' | 'POST'
  body?: unknown
}

export async function apiFetch<T>(url: string, options: RequestOptions = {}): Promise<T> {
  const headers = new Headers({ Accept: 'application/hal+json, application/problem+json' })
  if (options.body !== undefined) {
    headers.set('Content-Type', 'application/json')
  }
  const token = await tokenProvider()
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  let response: Response
  try {
    response = await fetch(url, {
      method: options.method ?? 'GET',
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
    })
  } catch {
    throw new ApiError(0, {
      title: 'Network error',
      detail: 'The server could not be reached. Check your connection and try again.',
    })
  }

  if (!response.ok) {
    throw new ApiError(response.status, await readProblem(response))
  }
  return (await response.json()) as T
}

async function readProblem(response: Response): Promise<ProblemDetail> {
  const contentType = response.headers.get('Content-Type') ?? ''
  if (contentType.includes('json')) {
    try {
      return (await response.json()) as ProblemDetail
    } catch {
      // fall through to a problem built from the status line
    }
  }
  return { status: response.status, title: response.statusText || `HTTP ${response.status}` }
}
