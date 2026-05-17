import request from '@/utils/request'

export function listPythonTestCases(taskId) {
  return request.get(`/python-labs/tasks/${taskId}/test-cases`)
}
