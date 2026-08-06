/**
 * ============================================================================
 * 文件：frontend/src/utils/appLinks.js
 * 作用：应用链接工具模块，处理跨窗口的 URL 生成和 Token 传递
 * ============================================================================
 *
 * 【教学知识点：双前端架构的跨窗口通信】
 *
 * 本项目采用双前端架构：
 * 1. 用户端（Shop）：http://localhost:3000
 * 2. 管理后台（Admin）：http://localhost:3001
 *
 * 两个前端应用运行在不同的端口（源），需要实现跨窗口通信：
 * - 用户端需要打开管理后台窗口
 * - 管理后台需要接收用户端的 Token
 *
 * 【教学知识点：同源策略（Same-Origin Policy）】
 *
 * 浏览器的同源策略限制不同源之间的交互：
 * - 源（Origin）= 协议 + 主机 + 端口
 * - http://localhost:3000 和 http://localhost:3001 是不同的源
 * - 不同源的 JavaScript 不能直接访问对方的 DOM、Cookie 等
 *
 * 跨源通信的解决方案：
 * 1. postMessage API：安全的跨源消息传递（本项目使用）
 * 2. CORS（跨源资源共享）：用于 API 请求
 * 3. JSONP：仅支持 GET 请求，有安全风险
 *
 * 【教学知识点：postMessage API 详解】
 *
 * postMessage 是 HTML5 提供的安全跨源通信机制：
 *
 * 发送消息：
 * targetWindow.postMessage(message, targetOrigin)
 * - targetWindow: 目标窗口的引用（如 window.open() 的返回值）
 * - message: 要发送的数据（会被结构化克隆）
 * - targetOrigin: 目标窗口的源（用于安全验证）
 *
 * 接收消息：
 * window.addEventListener('message', (event) => {
 *   event.data      // 消息数据
 *   event.origin    // 消息来源的源
 *   event.source    // 发送消息的窗口引用
 * })
 *
 * 【安全最佳实践】
 * 1. 始终验证 event.origin，只接受来自可信源的消息
 * 2. 始终指定 targetOrigin，不要使用 '*'
 * 3. 验证消息的格式和内容
 */

// ============================================================================
// 导入依赖
// ============================================================================

/**
 * 【教学知识点：认证工具函数】
 *
 * 从 auth.js 导入 getToken 函数，用于获取当前用户的 Token。
 * Token 会通过 postMessage 传递给管理后台。
 */
import { getToken } from './auth'

// ============================================================================
// 源地址配置
// ============================================================================

/**
 * 【教学知识点：环境变量（Environment Variables）】
 *
 * import.meta.env 是 Vite 提供的环境变量访问方式。
 *
 * Vite 内置的环境变量：
 * - import.meta.env.MODE: 当前模式（development/production）
 * - import.meta.env.DEV: 是否是开发模式（boolean）
 * - import.meta.env.PROD: 是否是生产模式（boolean）
 *
 * 自定义环境变量（需要以 VITE_ 开头）：
 * - import.meta.env.VITE_SHOP_ORIGIN: 用户端的源地址
 * - import.meta.env.VITE_ADMIN_ORIGIN: 管理后台的源地址
 *
 * 环境变量的配置方式：
 * 1. .env 文件：所有模式都会加载
 * 2. .env.local 文件：所有模式都会加载，被 gitignore
 * 3. .env.[mode] 文件：只在特定模式加载
 * 4. .env.[mode].local 文件：只在特定模式加载，被 gitignore
 *
 * 优先级：特定模式 > 通用模式，.local > 非 .local
 *
 * 示例 .env 文件：
 * VITE_SHOP_ORIGIN=http://localhost:3000
 * VITE_ADMIN_ORIGIN=http://localhost:3001
 *
 * 【教学知识点：默认值】
 *
 * import.meta.env.VITE_SHOP_ORIGIN || 'http://localhost:3000'
 *
 * 使用 || 提供默认值：
 * - 如果环境变量已设置，使用环境变量的值
 * - 如果环境变量未设置（undefined），使用默认值
 *
 * 这是一种常见的防御性编程模式，确保代码在任何环境下都能正常工作
 */
export const SHOP_ORIGIN = import.meta.env.VITE_SHOP_ORIGIN || 'http://localhost:3000'
export const ADMIN_ORIGIN = import.meta.env.VITE_ADMIN_ORIGIN || 'http://localhost:3001'
export const MERCHANT_ORIGIN = import.meta.env.VITE_MERCHANT_ORIGIN || 'http://localhost:3002'

// ============================================================================
// URL 构建函数
// ============================================================================

/**
 * 【教学知识点：buildShopUrl 函数】
 *
 * 功能：构建用户端的完整 URL。
 *
 * @param {string} [path='/'] - 路由路径
 * @returns {string} 完整的 URL
 *
 * 使用示例：
 * buildShopUrl('/products') → 'http://localhost:3000/products'
 * buildShopUrl('/') → 'http://localhost:3000/'
 *
 * 【教学知识点：URL 构造函数】
 *
 * new URL(path, base) 用于构建完整的 URL：
 * - path: 相对路径
 * - base: 基础 URL
 *
 * 示例：
 * new URL('/products', 'http://localhost:3000') → URL 对象
 * url.toString() → 'http://localhost:3000/products'
 *
 * 使用 URL 构造函数比手动拼接字符串更安全：
 * - 自动处理路径分隔符
 * - 自动处理查询参数
 * - 避免常见的拼接错误（如缺少 /）
 */
export const buildShopUrl = (path = '/') => {
  return new URL(path, SHOP_ORIGIN).toString()
}

/**
 * 【教学知识点：buildAdminUrl 函数】
 *
 * 功能：构建管理后台的完整 URL。
 *
 * @param {string} [path='/'] - 路由路径
 * @returns {string} 完整的 URL
 *
 * 使用示例：
 * buildAdminUrl('/products') → 'http://localhost:3001/products'
 * buildAdminUrl('/orders') → 'http://localhost:3001/orders'
 */
export const buildAdminUrl = (path = '/') => {
  return new URL(path, ADMIN_ORIGIN).toString()
}

/**
 * 功能：构建商家后台的完整 URL。
 *
 * @param {string} [path='/'] - 路由路径
 * @returns {string} 完整的 URL
 */
export const buildMerchantUrl = (path = '/') => {
  return new URL(path, MERCHANT_ORIGIN).toString()
}

// ============================================================================
// 管理后台窗口操作函数
// ============================================================================

/**
 * 【教学知识点：openAdminWindow 函数详解】
 *
 * 功能：打开管理后台窗口并使用 postMessage 传递 Token。
 *
 * @param {string} [path='/'] - 管理后台的路由路径
 * @returns {WindowProxy} 打开的窗口引用
 *
 * 使用示例：
 * const adminWindow = openAdminWindow('/products')
 * const adminWindow = openAdminWindow('/orders')
 *
 * 【教学知识点：window.open() 函数】
 *
 * window.open(url, target, features) 用于打开新窗口：
 * - url: 要打开的 URL
 * - target: 窗口目标
 *   - '_blank': 新窗口（默认）
 *   - '_self': 当前窗口
 *   - 'window_name': 命名窗口（同名窗口会复用）
 * - features: 窗口特性（如大小、位置等）
 *
 * 返回值：WindowProxy 对象，可以用于：
 * - 向新窗口发送消息（postMessage）
 * - 访问新窗口的某些属性（同源时）
 * - 关闭新窗口（close()）
 *
 * 【教学知识点：Token 传递的握手流程】
 *
 * 本函数实现了一个"握手"流程，确保 Token 安全传递：
 *
 * 1. 用户端打开管理后台窗口
 * 2. 用户端开始监听 ADMIN_READY 消息
 * 3. 管理后台加载完成后，发送 ADMIN_READY 消息
 * 4. 用户端收到消息后，发送 AUTH_TOKEN 消息
 * 5. 管理后台收到 Token 后，验证并保存
 *
 * 这种方式的优势：
 * - 管理后台明确表示"已准备好"，避免消息丢失
 * - 用户端在收到"已准备好"后才发送 Token，确保接收方已就绪
 * - 有超时机制，避免无限等待
 *
 * 【教学知识点：为什么使用 postMessage 而不是 URL 参数】
 *
 * URL 参数传递 Token 的问题：
 * - Token 会出现在浏览器地址栏
 * - Token 会出现在浏览器历史记录
 * - Token 可能被服务器日志记录
 * - Token 可能被 Referer 头泄露
 *
 * postMessage 的优势：
 * - Token 不会出现在任何 URL 或日志中
 * - 有 origin 验证，防止消息被恶意网站截获
 * - 可以传递复杂的数据结构
 */
export const openAdminWindow = (path = '/') => {
  // 构建管理后台的完整 URL
  const url = buildAdminUrl(path)

  // 打开新窗口，使用 'admin_window' 作为窗口名称
  // 如果已有同名窗口，会复用而不是创建新窗口
  const adminWindow = window.open(url, 'admin_window')

  /**
   * 【教学知识点：消息监听器】
   *
   * 监听管理后台发送的 ADMIN_READY 消息。
   *
   * 工作流程：
   * 1. 管理后台加载完成后，会发送 ADMIN_READY 消息
   * 2. 用户端收到消息后，验证来源并发送 AUTH_TOKEN
   *
   * 【安全考虑】
   * 必须验证 event.origin，只接受来自管理后台的消息。
   * 如果不验证，恶意网站可以发送伪造的 ADMIN_READY 消息，
   * 诱骗用户端发送 Token。
   */
  const handler = (event) => {
    // 验证消息来源
    if (event.origin !== ADMIN_ORIGIN) return

    // 检查消息类型
    if (event.data?.type === 'ADMIN_READY') {
      // 获取当前用户的 Token
      const token = getToken()

      // 如果 Token 存在且窗口未关闭，发送 Token
      if (token && adminWindow) {
        /**
         * 【教学知识点：发送 Token 消息】
         *
         * adminWindow.postMessage(message, targetOrigin)
         *
         * - message: 消息数据，包含类型和 Token
         * - targetOrigin: 目标窗口的源，必须与目标窗口的实际源匹配
         *
         * 消息格式约定：
         * {
         *   type: 'AUTH_TOKEN',
         *   token: 'xxx'
         * }
         *
         * 管理后台会监听这个消息，验证 Token 后保存到本地
         */
        adminWindow.postMessage({ type: 'AUTH_TOKEN', token }, ADMIN_ORIGIN)
      }

      // 移除消息监听器，避免重复处理
      window.removeEventListener('message', handler)
    }
  }

  // 添加消息监听器
  window.addEventListener('message', handler)

  /**
   * 【教学知识点：超时机制】
   *
   * 设置 5 秒超时，如果管理后台没有发送 ADMIN_READY 消息，
   * 则移除监听器，避免内存泄漏。
   *
   * setTimeout(callback, delay) 用于延迟执行：
   * - callback: 要执行的函数
   * - delay: 延迟时间（毫秒）
   *
   * 为什么需要超时？
   * - 管理后台可能加载失败
   * - 管理后台窗口可能被用户关闭
   * - 网络问题可能导致消息丢失
   *
   * 如果没有超时，监听器会一直存在，造成内存泄漏。
   */
  setTimeout(() => {
    window.removeEventListener('message', handler)
  }, 5000)

  // 返回窗口引用，调用方可以用于后续操作
  return adminWindow
}

/**
 * 功能：打开商家后台窗口并使用 postMessage 传递 Token。
 *
 * 与 openAdminWindow 类似，实现"商家后台就绪 → 用户端传递 Token"的握手流程：
 * 1. 用户端打开商家后台窗口
 * 2. 用户端监听 MERCHANT_READY 消息
 * 3. 商家后台加载完成后，发送 MERCHANT_READY 消息
 * 4. 用户端收到消息后，发送 AUTH_TOKEN 消息
 *
 * @param {string} [path='/'] - 商家后台的路由路径
 * @returns {WindowProxy} 打开的窗口引用
 */
export const openMerchantWindow = (path = '/') => {
  // 构建商家后台的完整 URL（如 http://localhost:3002/products）
  const url = buildMerchantUrl(path)

  // 打开新窗口，使用 'merchant_window' 作为窗口名称
  // 同名窗口会复用，避免打开多个商家后台
  const merchantWindow = window.open(url, 'merchant_window')

  /**
   * 消息监听器：等待商家后台发送 MERCHANT_READY 消息
   *
   * 工作流程：
   * 1. 商家后台加载完成后，发送 MERCHANT_READY 消息
   * 2. 用户端收到消息，验证来源
   * 3. 用户端通过 postMessage 发送 AUTH_TOKEN（含 Token）
   * 4. 商家后台接收 Token 并保存到本地
   *
   * 安全要点：
   * - 必须验证 event.origin，只接受商家后台源的消息
   * - 防止恶意网站伪造 MERCHANT_READY 诱骗 Token
   */
  const handler = (event) => {
    // 验证消息来源：只接受来自商家后台的消息
    if (event.origin !== MERCHANT_ORIGIN) return

    // 检查消息类型是否为商家后台就绪
    if (event.data?.type === 'MERCHANT_READY') {
      // 获取当前用户的 Token
      const token = getToken()
      // Token 存在且窗口未关闭时，发送 Token 给商家后台
      if (token && merchantWindow) {
        // 通过 postMessage 安全传递 Token
        // 第二个参数 MERCHANT_ORIGIN 是目标源，确保只有商家后台能接收
        merchantWindow.postMessage({ type: 'AUTH_TOKEN', token }, MERCHANT_ORIGIN)
      }
      // 移除监听器，避免重复处理（Token 只需传递一次）
      window.removeEventListener('message', handler)
    }
  }

  // 注册消息监听器，等待商家后台就绪
  window.addEventListener('message', handler)

  /**
   * 超时清理：5 秒后强制移除监听器
   *
   * 防止内存泄漏的场景：
   * - 商家后台加载失败
   * - 用户关闭了商家后台窗口
   * - 网络问题导致消息丢失
   *
   * 如果不清理，监听器会一直存在，造成内存泄漏。
   */
  setTimeout(() => {
    window.removeEventListener('message', handler)
  }, 5000)

  // 返回窗口引用，调用方可用于后续操作（如关闭窗口）
  return merchantWindow
}
