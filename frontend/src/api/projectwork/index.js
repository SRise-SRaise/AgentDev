import request from '@/utils/request'

// ---- 题目管理 ----
export function listProjectworks(params) {
  return request.get('/projectwork', { params })
}

export function getProjectwork(id) {
  return request.get(`/projectwork/${id}`)
}

export function createProjectwork(data) {
  return request.post('/projectwork', data)
}

export function updateProjectwork(id, data) {
  return request.put(`/projectwork/${id}`, data)
}

export function publishProjectwork(id) {
  return request.post(`/projectwork/${id}/publish`)
}

// ---- 提交管理 ----
export function listSubmissions(homeworkId, params) {
  return request.get(`/projectwork/${homeworkId}/submissions`, { params })
}

export function getSubmission(homeworkId, submissionId) {
  return request.get(`/projectwork/${homeworkId}/submissions/${submissionId}`)
}

export function uploadSubmission(homeworkId, formData) {
  return request.post(`/projectwork/${homeworkId}/submissions`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

// ---- Agent 评测 ----
export function triggerEval(homeworkId, submissionId) {
  return request.post(`/projectwork/${homeworkId}/submissions/${submissionId}/evaluate`)
}

export function batchTriggerEval(homeworkId, submissionIds) {
  return request.post(`/projectwork/${homeworkId}/evaluate/batch`, { submissionIds })
}

export function getEvalStatus(homeworkId, submissionId) {
  return request.get(`/projectwork/${homeworkId}/submissions/${submissionId}/eval-status`)
}

export function getEvalReport(homeworkId, submissionId) {
  return request.get(`/projectwork/${homeworkId}/submissions/${submissionId}/report`)
}

// ---- 教师复核 ----
export function saveReview(homeworkId, submissionId, data) {
  return request.post(`/projectwork/${homeworkId}/submissions/${submissionId}/review`, data)
}

// ---- 成绩汇总 ----
export function getGrades(homeworkId) {
  return request.get(`/projectwork/${homeworkId}/grades`)
}

export function exportGrades(homeworkId) {
  return request.get(`/projectwork/${homeworkId}/grades/export`, { responseType: 'blob' })
}

// ---- 学生端 ----
export function getStudentHomework(homeworkId) {
  return request.get(`/student/projectwork/${homeworkId}`)
}

export function getStudentSubmission(homeworkId) {
  return request.get(`/student/projectwork/${homeworkId}/submission`)
}

export function getStudentReport(homeworkId) {
  return request.get(`/student/projectwork/${homeworkId}/report`)
}

export function searchStudents(query) {
  return request.get('/students/search', { params: { q: query } })
}
