/**
 * 谈话文书导出下载器。
 *
 * 项目统一的 request 拦截器只返回 Blob，读不到响应头，因此导出与附件下载改用原生 axios：
 * 文件名一律以响应头 download-filename（UTF-8 百分号编码）为准，只有该头缺失时才使用兜底名，
 * 避免前端与后端两套命名规则漂移。响应的 Content-Type 为 JSON 时视为失败，展示后端 msg，
 * 不能把错误 JSON 保存成文件。
 */
import axios from 'axios'
import { Loading } from 'element-ui'
import { saveAs } from 'file-saver'
import { getToken } from '@/utils/auth'
import { listAttachment } from '@/api/talk/talkAttachment'

const BASE_API = process.env.VUE_APP_BASE_API || ''
const DEFAULT_ERROR = '导出失败，请稍后重试'

function isJsonBlob(blob) {
  const type = String((blob && blob.type) || '').split(';')[0].trim().toLowerCase()
  return type === 'application/json' || type.endsWith('+json')
}

async function readMessage(blob) {
  if (!blob || typeof blob.text !== 'function') {
    return ''
  }
  try {
    const text = await blob.text()
    if (!text) {
      return ''
    }
    const data = JSON.parse(text)
    return (data && (data.msg || data.message)) || ''
  } catch (error) {
    return ''
  }
}

async function requestBlob(config) {
  let response
  try {
    response = await axios({
      method: config.method || 'get',
      url: (BASE_API || '') + config.url,
      params: config.params,
      data: config.data,
      responseType: 'blob',
      headers: { 'Authorization': 'Bearer ' + getToken() }
    })
  } catch (error) {
    // 非 2xx 的失败响应同样是 Blob（JSON），优先展示后端 msg。
    const message = await readMessage(error && error.response && error.response.data)
    throw new Error(message || DEFAULT_ERROR)
  }
  if (isJsonBlob(response.data)) {
    throw new Error((await readMessage(response.data)) || DEFAULT_ERROR)
  }
  return { blob: response.data, headers: response.headers || {} }
}

function resolveFileName(headers, fallbackName) {
  const encoded = headers['download-filename']
  if (!encoded) {
    return fallbackName
  }
  try {
    return decodeURIComponent(encoded) || fallbackName
  } catch (error) {
    return encoded
  }
}

function downloadBlob(config, fallbackName) {
  return requestBlob(config).then(({ blob, headers }) => {
    // 附件可能是图片/PDF，不能按 PK 文件头判断，只要不是 JSON 错误就保存。
    saveAs(blob, resolveFileName(headers, fallbackName))
  })
}

/**
 * 导出单个会话：个别谈话为 1 个 docx，集体谈话为整场 ZIP。
 */
export function downloadSessionArchive(sessionId, fallbackName) {
  return downloadBlob({ url: '/ruoyi-system/talksession/exportDocx/' + sessionId }, fallbackName)
}

/**
 * 导出单个学生在指定会话中的谈话记录（1 个 docx）。
 */
export function downloadStudentDocx(sessionId, studentId, fallbackName) {
  return downloadBlob({
    url: '/ruoyi-system/talksession/exportDocx/' + sessionId + '/student/' + studentId
  }, fallbackName)
}

/**
 * 按谈话记录导出（1 个 docx）。
 */
export function downloadRecordDocx(recordId, fallbackName) {
  return downloadBlob({ url: '/ruoyi-system/talksession/exportDocx/record/' + recordId }, fallbackName)
}

/**
 * 批量导出会话：一次请求返回一个扁平 ZIP。
 */
export function downloadSessionBatch(sessionIds, fallbackName) {
  return downloadBlob({
    method: 'post',
    url: '/ruoyi-system/talksession/exportDocx/batch',
    data: { sessionIds }
  }, fallbackName)
}

/**
 * 按选中的学生记录批量导出：一次请求返回一个扁平 ZIP。
 */
export function downloadRecordBatch(recordIds, fallbackName) {
  return downloadBlob({
    method: 'post',
    url: '/ruoyi-system/talksession/exportDocx/records/batch',
    data: { recordIds }
  }, fallbackName)
}

/**
 * 导出流程内下载单个附件：带 exportName=1，使用导出命名（页面附件区下载按钮不受影响）。
 */
export function downloadAttachmentForExport(attachmentId, fallbackName) {
  return downloadBlob({
    url: '/ruoyi-system/talkattachment/download/' + attachmentId,
    params: { exportName: 1 }
  }, fallbackName)
}

function loadAttachments(sessionId) {
  return listAttachment(sessionId).then(response => {
    const attachments = response && response.data
    return Array.isArray(attachments) ? attachments : []
  })
}

/**
 * 导出 1 个 Word 后按顺序下载该场谈话的全部附件。
 *
 * - exportWord 负责下载 Word，失败时抛出 Error，此时不继续下载附件；
 * - 附件列表读取失败（如权限不足）只提示警告，Word 已下载，不中断；
 * - 单个附件失败不中断其余附件，结束后汇总“完成 X/Y，失败：附件名（原因）”；
 * - 整个流程只创建一个全屏 loading，进度文案用 setText 刷新，finally 中关闭。
 *
 * @param {Object} vm 当前 Vue 实例，用于 $modal 的提示
 * @param {number} sessionId 附件所属会话
 * @param {Function} exportWord 下载 Word 的函数（成功即已保存文件，失败抛出 Error）
 */
export async function exportWordWithAttachments(vm, sessionId, exportWord) {
  const modal = vm.$modal
  const loading = Loading.service({
    lock: true,
    text: '正在导出 Word 文件…',
    spinner: 'el-icon-loading',
    background: 'rgba(0, 0, 0, 0.7)'
  })
  try {
    await exportWord()
    let attachments
    try {
      attachments = await loadAttachments(sessionId)
    } catch (error) {
      modal.msgWarning('附件列表读取失败，已仅导出 Word')
      return
    }
    if (!attachments.length) {
      modal.msgSuccess('导出成功')
      return
    }
    const total = attachments.length + 1
    const failed = []
    for (let index = 0; index < attachments.length; index++) {
      const attachment = attachments[index] || {}
      const name = attachment.fileName || ('附件' + (index + 1))
      loading.setText('正在导出 ' + (index + 2) + '/' + total + '…')
      if (!attachment.attachmentId) {
        failed.push(name + '（附件编号缺失）')
        continue
      }
      try {
        await downloadAttachmentForExport(attachment.attachmentId, name)
      } catch (error) {
        failed.push(name + '（' + ((error && error.message) || '下载失败') + '）')
      }
    }
    const succeeded = attachments.length - failed.length
    if (failed.length) {
      modal.msgWarning('完成 ' + succeeded + '/' + attachments.length + '，失败：' + failed.join('、'))
    } else {
      modal.msgSuccess('导出成功')
    }
  } finally {
    loading.close()
  }
}
