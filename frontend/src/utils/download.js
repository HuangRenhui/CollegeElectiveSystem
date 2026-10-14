import { ElMessage } from 'element-plus'

/**
 * 触发浏览器下载一个二进制响应。
 *
 * <p>后端导出接口返回原始字节流，前端必须用 {@code responseType: 'blob'} 请求，
 * 否则 axios 会按文本解析，二进制内容被破坏（中文与换行会乱码）。</p>
 *
 * <p>文件名优先取响应头 {@code Content-Disposition}，取不到时用调用方传入的
 * 兜底名。注意后端用的是 RFC 5987 的 {@code filename*=UTF-8''...} 形式，
 * 需要手动解码，浏览器不会自动处理。</p>
 *
 * @param {object} response axios 响应（含 data 与 headers）
 * @param {string} fallbackName 兜底文件名
 */
export function downloadBlob(response, fallbackName = 'download.xlsx') {
  const blob = response?.data
  if (!blob) {
    ElMessage.error('导出内容为空')
    return
  }

  // 后端在业务失败时也会返回 JSON（而非二进制），此时 blob 里其实是 JSON 文本。
  // 内容类型不是表格时，读出来提示用户，避免下载一个满是报错信息的"Excel"。
  const contentType = response.headers?.['content-type'] || ''
  if (contentType.includes('application/json')) {
    const reader = new FileReader()
    reader.onload = () => {
      try {
        const body = JSON.parse(reader.result)
        ElMessage.error(body.message || '导出失败')
      } catch {
        ElMessage.error('导出失败')
      }
    }
    reader.readAsText(blob)
    return
  }

  const fileName = parseFileName(response.headers?.['content-disposition']) || fallbackName

  const url = window.URL.createObjectURL(new Blob([blob]))
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  // 释放对象 URL，否则大文件会一直占着内存直到页面刷新
  window.URL.revokeObjectURL(url)
}

/**
 * 从 Content-Disposition 解析文件名。
 *
 * <p>优先解析 RFC 5987 的 {@code filename*=UTF-8''%E4%B8%AD%E6%96%87.xlsx}
 * （中文必须走这种形式），其次才是普通的 {@code filename="xxx"}。</p>
 */
function parseFileName(disposition) {
  if (!disposition) return ''

  // filename*=UTF-8''xxx
  const utf8Match = /filename\*=UTF-8''([^;]+)/i.exec(disposition)
  if (utf8Match) {
    try {
      return decodeURIComponent(utf8Match[1])
    } catch {
      return utf8Match[1]
    }
  }

  // filename="xxx" 或 filename=xxx
  const plainMatch = /filename="?([^";]+)"?/i.exec(disposition)
  if (plainMatch) {
    return plainMatch[1]
  }
  return ''
}
