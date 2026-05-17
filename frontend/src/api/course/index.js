import request from '@/utils/request'

export function getCourseObjectives() {
  return request.get('/course/objectives')
}

export function syncExperimentScores(data) {
  return request.post('/course/scores/sync-experiment', data)
}
