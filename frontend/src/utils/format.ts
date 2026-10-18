const money = new Intl.NumberFormat('en-GB', { style: 'currency', currency: 'EUR' })
const dateTime = new Intl.DateTimeFormat('en-GB', { dateStyle: 'medium', timeStyle: 'short' })

export function formatMoney(amount: number): string {
  return money.format(amount)
}

export function formatDateTime(iso: string): string {
  return dateTime.format(new Date(iso))
}

/** Value for a datetime-local input, in the user's time zone (YYYY-MM-DDTHH:mm). */
export function toLocalInput(date: Date): string {
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60_000)
  return local.toISOString().slice(0, 16)
}
