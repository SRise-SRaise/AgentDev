import request from '@/utils/request'

export function listPythonTasks(params) {
  return request.get('/python-labs/tasks', { params })
}
