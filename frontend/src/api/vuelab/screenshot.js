import request from '@/utils/request'

export function listVueScreenshots(submissionId) {
  return request.get(`/vue-labs/submissions/${submissionId}/screenshots`)
}
