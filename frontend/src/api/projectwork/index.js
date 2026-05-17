import request from '@/utils/request'

export function listProjectworks(params) {
  return request.get('/projectwork', { params })
}
