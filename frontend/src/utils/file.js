export function getFileName(path = '') {
  const normalized = String(path)
  const parts = normalized.split('/')
  return parts[parts.length - 1] || ''
}
