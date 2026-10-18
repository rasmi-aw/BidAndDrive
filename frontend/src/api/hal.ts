import type { HalResource, PagedResource } from './types'

export function linkTo(resource: HalResource | null | undefined, rel: string): string | undefined {
  return resource?._links[rel]?.href
}

/** For actions the UI only offers when the link is present; a missing link is a programming error. */
export function requireLink(resource: HalResource | null | undefined, rel: string): string {
  const href = linkTo(resource, rel)
  if (!href) {
    throw new Error(`the resource has no '${rel}' link`)
  }
  return href
}

export function embedded<T>(page: PagedResource<T>, rel: string): T[] {
  return page._embedded?.[rel] ?? []
}
