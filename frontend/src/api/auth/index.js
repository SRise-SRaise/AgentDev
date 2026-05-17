import request from '@/utils/request'

export function login(data) {
  return request.post('/user/login', data)
}

export function getLoginUser() {
  return request.get('/user/get/login')
}
