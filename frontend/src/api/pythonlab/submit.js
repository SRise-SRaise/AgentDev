import request from '@/utils/request'

export function submitPythonTask(taskId, data) {
  return request.post(`/python-labs/tasks/${taskId}/submissions`, data)
}
