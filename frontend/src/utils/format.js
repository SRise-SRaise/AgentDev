export function formatScore(value) {
  if (value === null || value === undefined || value === '') {
    return '--'
  }
  return Number(value).toFixed(2)
}
