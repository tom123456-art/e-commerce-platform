export function formatPrice(price){
    return Number(price).toFixed(2)
}

export const formatDate = (dateString) => {
  // 空值检查：未传入日期则返回占位符
  if (!dateString) return '-'
  // 用 Date 对象解析并转为本地时间字符串
  return new Date(dateString).toLocaleString()
}