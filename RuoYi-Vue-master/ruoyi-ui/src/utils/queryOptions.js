// Sort only sibling groups containing a year node; leave all other levels untouched.
export function sortGradeNodes(nodes) {
  const year = node => {
    const match = /^(\d{4})级$/.exec((node.deptName || node.label || '').trim())
    return match ? Number(match[1]) : null
  }
  function walk(items) {
    const copy = (items || []).map(node => ({ ...node, ...(node.children ? { children: walk(node.children) } : {}) }))
    if (copy.some(node => year(node) !== null)) {
      return copy.map((node, index) => ({ node, index, year: year(node) }))
        .sort((a, b) => (b.year === null ? -1 : b.year) - (a.year === null ? -1 : a.year) || a.index - b.index)
        .map(item => item.node)
    }
    return copy
  }
  return walk(nodes)
}

export function validateDateRange(start, end) {
  if (!start && !end) return ''
  if (!start || !end) return '请选择完整的开始和结束日期'
  const valid = value => {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return false
    const date = new Date(value + 'T00:00:00Z')
    return !Number.isNaN(date.getTime()) && date.toISOString().slice(0, 10) === value
  }
  if (!valid(start) || !valid(end)) return '日期格式无效'
  return start > end ? '开始日期不能晚于结束日期' : ''
}
