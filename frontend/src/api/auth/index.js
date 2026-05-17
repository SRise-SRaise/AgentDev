import request from '@/utils/request'

export function registerStudent(data) {
  return request.post('/user/register/student', data)
}

export function login(data) {
  return request.post('/user/login', data)
}

export function logout() {
  return request.post('/user/logout')
}

export function getLoginUser() {
  return request.get('/user/get/login')
}
