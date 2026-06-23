export function formatDateTime(value?: string | null) {
  if (!value) {
    return ''
  }
  const normalized = value.includes('T') ? value.replace('T', ' ') : value
  if (/^\d{4}-\d{2}-\d{2}$/.test(normalized)) {
    return `${normalized} 00:00:00`
  }
  return normalized.slice(0, 19)
}
