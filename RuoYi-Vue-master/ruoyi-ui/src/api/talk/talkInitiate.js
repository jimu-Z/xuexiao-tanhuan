import request from '@/utils/request'

export function getSpeakerOptions(studentId) {
  return request({ url: '/ruoyi-system/talksession/speaker-options', method: 'get', params: { studentId } })
}

export function createTalk(data) {
  return request({
    url: '/ruoyi-system/talksession/create',
    method: 'post',
    data: data
  })
}
