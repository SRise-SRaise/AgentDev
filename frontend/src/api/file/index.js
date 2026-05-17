import request from '@/utils/request'

export function uploadFile(formData, biz) {
  return request.post(`/file/upload?biz=${biz}`, formData)
}
