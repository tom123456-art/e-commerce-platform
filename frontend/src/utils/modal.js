/**
 * 弹窗工具模块（纯 DOM 实现，零依赖）。
 *
 * 【为什么不用原生 alert/confirm？】
 * 1. 样式丑陋，与页面风格不统一
 * 2. 会阻塞 JavaScript 执行（同步阻塞）
 * 3. 无法自定义按钮文案和标题
 *
 * 【为什么不用 Element Plus？】
 * 本项目希望前端骨架尽量轻量，弹窗只在少数场景使用，
 * 引入整个 UI 框架不划算。纯 DOM 实现可控、零依赖。
 */

/**
 * 【教学知识点：HTML 转义函数】
 *
 * 将字符串中的特殊 HTML 字符转换为实体，防止 XSS 攻击。
 * 需要转义的字符：& < > " '
 *
 * 【教学知识点：XSS 攻击】
 * XSS（Cross-Site Scripting）通过在页面注入恶意脚本，可窃取 Cookie/Token、
 * 伪造请求、篡改页面。本函数使用 DOM API 转义，比手动 replace 更可靠：
 * 1. 创建一个 div 元素
 * 2. 设置 textContent（浏览器会自动转义特殊字符）
 * 3. 读取 innerHTML（获取转义后的安全内容）
 *
 * @param {string} str - 需要转义的字符串
 * @returns {string} 转义后的安全字符串
 */
function escapeHtml(str) {
  if (!str) return ''
  const div = document.createElement('div')
  div.textContent = str
  return div.innerHTML
}

/**
 * 创建模态框容器：遮罩层（半透明背景）+ 模态框卡片。
 * 返回 { overlay, modal } 两个 DOM 元素，供调用方填充内容。
 */
function createModalContainer() {
  // 遮罩层：固定定位铺满视口，点击可关闭
  const overlay = document.createElement('div')
  overlay.style.cssText = 'position:fixed;top:0;left:0;width:100%;height:100%;background:rgba(0,0,0,0.45);z-index:9999;display:flex;align-items:center;justify-content:center;'

  // 模态框卡片：白色圆角，限宽
  const modal = document.createElement('div')
  modal.style.cssText = 'background:#fff;border-radius:16px;padding:28px 32px;max-width:420px;width:90%;box-shadow:0 20px 60px rgba(0,0,0,0.2);font-family:system-ui,-apple-system,sans-serif;'

  overlay.appendChild(modal)
  document.body.appendChild(overlay)
  return { overlay, modal }
}

/**
 * 显示确认对话框，返回 Promise<boolean>。
 *
 * @param {string} message - 确认提示文本
 * @param {Object} [options] - 可选配置
 * @param {string} [options.confirmText='确认'] - 确认按钮文本
 * @param {string} [options.cancelText='取消'] - 取消按钮文本
 * @param {string} [options.title='操作确认'] - 对话框标题
 * @returns {Promise<boolean>} 用户点击确认返回 true，取消返回 false
 *
 * 使用示例：
 *   const confirmed = await confirmDialog('确认删除该商品？')
 *   if (confirmed) { 执行删除 }
 *
 * 【安全】所有用户输入都通过 escapeHtml() 转义后再拼入 innerHTML，防止 XSS。
 */
export function confirmDialog(message, options = {}) {
  return new Promise((resolve) => {
    const { overlay, modal } = createModalContainer()
    const title = options.title || '操作确认'

    // 构建 HTML：标题 + 提示文案 + 按钮组（全部经过 escapeHtml 转义）
    modal.innerHTML = `
      <h3 style="margin:0 0 14px 0;font-size:18px;font-weight:700;color:#1e293b;">${escapeHtml(title)}</h3>
      <p style="margin:0 0 22px 0;font-size:15px;color:#475569;line-height:1.6;">${escapeHtml(message)}</p>
      <div style="display:flex;gap:12px;justify-content:flex-end;">
        <button class="modal-cancel-btn" style="padding:10px 22px;border-radius:10px;border:1px solid #cbd5e1;background:#fff;color:#475569;font-size:14px;font-weight:600;cursor:pointer;">${escapeHtml(options.cancelText || '取消')}</button>
        <button class="modal-confirm-btn" style="padding:10px 22px;border-radius:10px;border:none;background:#4f7cff;color:#fff;font-size:14px;font-weight:600;cursor:pointer;">${escapeHtml(options.confirmText || '确认')}</button>
      </div>
    `

    const confirmBtn = modal.querySelector('.modal-confirm-btn')
    const cancelBtn = modal.querySelector('.modal-cancel-btn')

    // 关闭：移除 DOM + resolve 结果（移除 overlay 会自动清理其子元素的事件监听）
    const close = (result) => {
      document.body.removeChild(overlay)
      resolve(result)
    }

    confirmBtn.addEventListener('click', () => close(true))
    cancelBtn.addEventListener('click', () => close(false))

    // 点击遮罩层背景关闭（检查 e.target === overlay 避免点击模态框内容误关）
    overlay.addEventListener('click', (e) => {
      if (e.target === overlay) close(false)
    })

    // ESC 键关闭（document 上的监听需手动清理，避免内存泄漏）
    const handleEsc = (e) => {
      if (e.key === 'Escape') {
        close(false)
        document.removeEventListener('keydown', handleEsc)
      }
    }
    document.addEventListener('keydown', handleEsc)

    // 聚焦确认按钮，支持 Enter 直接确认（无障碍设计）
    confirmBtn.focus()
  })
}

/**
 * 显示消息提示框，返回 Promise<void>。
 *
 * @param {string} message - 提示文本
 * @param {Object} [options] - 可选配置
 * @param {string} [options.buttonText='确定'] - 按钮文本
 * @param {string} [options.title='提示'] - 对话框标题
 * @returns {Promise<void>} 用户点击确定后 resolve
 *
 * 使用示例：
 *   await alertMessage('操作成功！')
 *
 * 与 confirmDialog 的区别：只有一个"确定"按钮，用于信息提示。
 */
export function alertMessage(message, options = {}) {
  return new Promise((resolve) => {
    const { overlay, modal } = createModalContainer()
    const title = options.title || '提示'

    modal.innerHTML = `
      <h3 style="margin:0 0 14px 0;font-size:18px;font-weight:700;color:#1e293b;">${escapeHtml(title)}</h3>
      <p style="margin:0 0 22px 0;font-size:15px;color:#475569;line-height:1.6;">${escapeHtml(message)}</p>
      <div style="display:flex;justify-content:flex-end;">
        <button class="modal-ok-btn" style="padding:10px 22px;border-radius:10px;border:none;background:#4f7cff;color:#fff;font-size:14px;font-weight:600;cursor:pointer;">${escapeHtml(options.buttonText || '确定')}</button>
      </div>
    `

    const okBtn = modal.querySelector('.modal-ok-btn')

    const close = () => {
      document.body.removeChild(overlay)
      resolve()
    }

    okBtn.addEventListener('click', close)

    overlay.addEventListener('click', (e) => {
      if (e.target === overlay) close()
    })

    const handleEsc = (e) => {
      if (e.key === 'Escape') {
        close()
        document.removeEventListener('keydown', handleEsc)
      }
    }
    document.addEventListener('keydown', handleEsc)

    okBtn.focus()
  })
}
