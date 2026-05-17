import request from '@/utils/request'

export function reviewVueScore(submissionId, data) {
  return request.post(`/vue-labs/submissions/${submissionId}/review-score`, data)
}
