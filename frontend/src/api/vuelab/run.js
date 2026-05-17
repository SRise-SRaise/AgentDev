import request from '@/utils/request'

export function runVueSubmission(submissionId) {
  return request.post(`/vue-labs/submissions/${submissionId}/run`)
}
