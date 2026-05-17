import request from '@/utils/request'

export function reviewPythonScore(submissionId, data) {
  return request.post(`/python-labs/submissions/${submissionId}/review-score`, data)
}
