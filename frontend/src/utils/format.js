const MONEY_LOCALE = 'en-KE'
const MONEY_CURRENCY = 'KES'
const moneyFormatter = new Intl.NumberFormat(MONEY_LOCALE, {
  style: 'currency',
  currency: MONEY_CURRENCY,
  maximumFractionDigits: 2
})

export function formatMoney(value) {
  const numeric = Number(value || 0)
  return moneyFormatter.format(numeric)
}

export function formatDateTime(value) {
  if (!value) return '—'
  return new Intl.DateTimeFormat('en-US', {
    dateStyle: 'medium',
    timeStyle: 'short'
  }).format(new Date(value))
}

export function toneForStatus(status = '') {
  const value = String(status).toUpperCase()
  if (value.includes('RECEIVED') || value.includes('COMPLETED') || value.includes('APPROVED') || value.includes('SUCCESS')) {
    return 'teal'
  }
  if (value.includes('REJECT') || value.includes('VOID') || value.includes('FAILED') || value.includes('ERROR')) {
    return 'danger'
  }
  if (value.includes('TRANSIT') || value.includes('PENDING') || value.includes('SUBMITTED') || value.includes('DRAFT')) {
    return 'warning'
  }
  return 'slate'
}

export function currencyOrDash(value) {
  if (value === null || value === undefined || value === '') return '—'
  return formatMoney(value)
}
