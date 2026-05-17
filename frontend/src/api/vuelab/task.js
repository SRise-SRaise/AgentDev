import request from '@/utils/request'

export function listVueTasks(params) {
  return request.get('/vue-labs/tasks', { params })
}
