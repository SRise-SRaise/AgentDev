import request from '@/utils/request'

export function submitVueTask(taskId, data) {
  return request.post(`/vue-labs/tasks/${taskId}/submissions`, data)
}
