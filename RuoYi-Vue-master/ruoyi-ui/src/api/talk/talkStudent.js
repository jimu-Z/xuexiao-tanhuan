import request from '@/utils/request'

export function getNations() {
  return request({ url: '/ruoyi-system/talk/nations', method: 'get' })
}

export function previewStudentPhotos(data) {
  return request({ url: '/ruoyi-system/student-photo/preview', method: 'post', data, timeout: 120000 })
}

export function importStudentPhotos(data, replace) {
  return request({ url: '/ruoyi-system/student-photo/execute', method: 'post', params: { replace }, data, timeout: 600000 })
}

export function getStudentPhoto(studentId) {
  return request({ url: '/ruoyi-system/student-photo/' + studentId, method: 'get', responseType: 'blob' })
}

// 列表与导出共用期间未谈筛选，保留其他字段并避免日期数组绑定失效。
export function buildStudentQuery(query) {
  const { dateRange, ...filters } = query
  const params = { ...query.params }
  delete params.beginTime
  delete params.endTime
  if (Array.isArray(dateRange) && dateRange.length === 2) {
    params.beginTime = dateRange[0]
    params.endTime = dateRange[1]
  }
  const deptId = Array.isArray(query.deptId) ? (query.deptId.length ? query.deptId[query.deptId.length - 1] : null) : query.deptId
  return { ...filters, deptId, params }
}

// 查询学生信息管理列表
export function listTalk(query) {
  return request({
    url: '/ruoyi-system/talk/list',
    method: 'get',
    params: buildStudentQuery(query)
  })
}

// 查询学生信息管理详细
export function getTalk(studentId) {
  return request({
    url: '/ruoyi-system/talk/byId/' + studentId,
    method: 'get'
  })
}

// 新增学生信息管理
export function addTalk(data) {
  return request({
    url: '/ruoyi-system/talk',
    method: 'post',
    data: data
  })
}

// 修改学生信息管理
export function updateTalk(data) {
  return request({
    url: '/ruoyi-system/talk',
    method: 'put',
    data: data
  })
}

// 删除学生信息管理
export function delTalk(studentId) {
  return request({
    url: '/ruoyi-system/talk/' + studentId,
    method: 'delete'
  })
}

// 查询学生详情（含历史谈话记录）
export function getStudentDetail(studentId) {
  return request({
    url: '/ruoyi-system/talk/detail/' + studentId,
    method: 'get'
  })
}

// 导入预览
export function importPreview(data) {
  return request({
    url: '/ruoyi-system/talk/import/preview',
    method: 'post',
    timeout: 120000,
    data: data
  })
}

// 执行导入
export function importExecute(data, importMode) {
  return request({
    url: '/ruoyi-system/talk/import/execute',
    method: 'post',
    // 批量导入逐行提交并可能创建学生账号，不能沿用普通查询的10秒上限。
    timeout: 600000,
    params: { importMode: importMode || 'skip' },
    data: data
  })
}

// 获取班级树
export function getDeptTree() {
  return request({
    url: '/ruoyi-system/talk/deptTree',
    method: 'get'
  })
}

// 查询未谈学生
export function listUntalked(params) {
  return request({
    url: '/ruoyi-system/talk/untalked',
    method: 'get',
    params: params
  })
}
