/**
 * 商品分类和图片管理工具
 */

export const PRODUCT_CATEGORIES = [
  { id: 1, label: '手机数码' },
  { id: 2, label: '电脑办公' },
  { id: 3, label: '智能家电' },
  { id: 4, label: '居家生活' },
  { id: 5, label: '运动户外' },
  { id: 6, label: '影音娱乐' }
]

/**
 * 根据分类 ID 获取分类名称
 */
export function getCategoryLabel(categoryId) {
  const cat = PRODUCT_CATEGORIES.find(c => c.id === Number(categoryId))
  return cat ? cat.label : '未分类'
}

/**
 * 解析商品图片（如果是占位图则生成 SVG）
 */
export function resolveProductImage(product) {
  const image = product?.image || ''
  if (image && !image.includes('placeholder')) {
    return image
  }
  // 返回默认占位图
  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(
    `<svg width="336" height="256" xmlns="http://www.w3.org/2000/svg">
      <rect width="336" height="256" rx="28" fill="#E7F0FF"/>
      <text x="168" y="128" text-anchor="middle" fill="#4F7CFF" font-size="20">${product?.name || '商品'}</text>
    </svg>`
  )}`
}