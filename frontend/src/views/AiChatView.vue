<template>
  <div class="page-shell ai-chat">
    <div class="page-header">
      <div>
        <span class="eyebrow">AI Customer Service</span>
        <h1>AI 客服</h1>
        <p class="page-subtitle">智能客服助手，为您解答商品咨询、订单查询、物流跟踪、售后服务等问题。</p>
      </div>
    </div>

    <div class="chat-container">
      <!-- 消息列表区域：ref 用于获取 DOM 实现自动滚动 -->
      <div class="chat-messages" ref="messagesContainer">
        <!-- 空状态：快捷问题按钮 -->
        <div v-if="messages.length === 0" class="chat-empty">
          <div class="chat-empty-icon">💬</div>
          <h3>欢迎来到优选商城</h3>
          <p>我是AI客服助手，可以帮您解答商品推荐、订单查询、物流跟踪、售后服务等问题。</p>
          <div class="quick-questions">
            <button v-for="q in quickQuestions" :key="q" class="quick-btn" @click="sendQuickQuestion(q)">
              {{ q }}
            </button>
          </div>
        </div>

        <!-- 消息列表：v-for 遍历，根据 role 切换样式 -->
        <div v-for="msg in messages" :key="msg.id" class="chat-message" :class="msg.role">
          <div class="message-avatar">{{ msg.role === 'user' ? '👤' : '🤖' }}</div>
          <div class="message-content">
            <div class="message-text">{{ msg.content }}</div>
            <div class="message-meta">
              <!-- fallback 标识：区分 AI 回复 / 模板回复 -->
              <span v-if="msg.fallback !== undefined" class="pill" :class="msg.fallback ? 'pill-template' : 'pill-ai'">
                {{ msg.fallback ? '模板回复' : 'AI 回复' }}
              </span>
            </div>
          </div>
        </div>

        <!-- 加载中动画 -->
        <div v-if="loading" class="chat-message assistant">
          <div class="message-avatar">🤖</div>
          <div class="message-content">
            <div class="message-text typing">正在输入中...</div>
          </div>
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="chat-input-area">
        <div class="input-wrapper">
          <!-- @keydown.enter.exact.prevent：单独按 Enter 发送，Shift+Enter 换行 -->
          <textarea
            v-model="inputMessage"
            placeholder="输入您的问题，按 Enter 发送..."
            rows="1"
            @keydown.enter.exact.prevent="sendMessage"
            :disabled="loading"
          />
          <button class="send-btn" :disabled="!inputMessage.trim() || loading" @click="sendMessage">
            发送
          </button>
        </div>
        <p v-if="error" class="chat-error">{{ error }}</p>
      </div>
    </div>
  </div>
</template>

<script>
import { nextTick, onMounted, ref } from 'vue'
import http from '../utils/http'

// 快捷问题配置：点击直接发送
const quickQuestions = [
  '有什么热门商品推荐？',
  '如何查看我的订单？',
  '支持退换货吗？',
  '发货要多久？'
]

export default {
  name: 'AiChatView',
  setup() {
    // ========================================================================
    // 1. 响应式状态
    // ========================================================================
    const messages = ref([])              // 消息列表
    const inputMessage = ref('')          // 输入框内容
    const loading = ref(false)            // 加载状态
    const error = ref('')                 // 错误信息
    const sessionId = ref('')             // 会话 ID（后端维护多轮上下文）
    const messagesContainer = ref(null)   // 模板 ref：获取消息容器 DOM

    // ========================================================================
    // 2. 工具函数
    // ========================================================================

    /** 生成会话 ID：时间戳 + 随机串 */
    const generateSessionId = () => {
      return 'chat_' + Date.now() + '_' + Math.random().toString(36).substring(2, 9)
    }

    /**
     * 滚动到底部
     * 为什么用 nextTick？messages.push() 后 DOM 还没更新，scrollHeight 是旧值
     * nextTick 确保 DOM 更新后再读取 scrollHeight
     */
    const scrollToBottom = async () => {
      await nextTick()
      if (messagesContainer.value) {
        messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
      }
    }

    // ========================================================================
    // 3. 核心方法
    // ========================================================================

    /**
     * 发送消息
     * 流程：push 用户消息 → 滚动 → POST /ai/chat → push AI 回复 → 滚动
     */
    const sendMessage = async () => { // 发送消息的核心异步方法
      // 获取输入框输入的内容
      const text = inputMessage.value.trim()
      // 如果没有消息或者是加载中的时候，不能发送
      if(!text || loading.value) return
      // 添加用户的消息到列表中
      messages.value.push({
        role: 'user',
        content: text
      })
      // 清空输入框
      inputMessage.value = ''
      loading.value = true
      error.value = ''
      await scrollToBottom() // 滑动到底部显示最新消息
      try {
        const response = await http.post('/ai/chat', {
          message: text,   // 用户输入的消息
          sessionId: sessionId.value  // 当前会话id
        })
        // 更新SessionId，用后端返回的sessionId更新本地
        sessionId.value = response.data.sessionId 
        // 添加AI的回复，带有fallback标识
        messages.value.push({
          role: 'assistant',
          content: response.data.reply,
          fallback: response.data.fallback
        })
      } catch (err) {
        error.value = err.message || '发送失败，请重试'
      } finally {
        loading.value = false
        await scrollToBottom()
      }
    }

    /** 快捷问题：填入并立即发送 */
    const sendQuickQuestion = (question) => {
      // 将快捷问题填入输入框
      inputMessage.value = question
      sendMessage()
    }

    // ========================================================================
    // 4. 生命周期
    // ========================================================================
    onMounted(() => {
      sessionId.value = generateSessionId()  // 页面加载生成新会话
    })

    return {
      messages,
      inputMessage,
      loading,
      error,
      messagesContainer,
      quickQuestions,
      sendMessage,
      sendQuickQuestion
    }
  }
}
</script>

<style scoped>
.ai-chat { max-width: 800px; margin: 0 auto; padding: 20px; }
.chat-container {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 260px);
  min-height: 500px;
  border: 1px solid #eee;
  border-radius: 20px;
  background: #fff;
  overflow: hidden;
}
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.chat-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  gap: 16px;
  text-align: center;
  color: #666;
}
.chat-empty-icon { font-size: 48px; }
.quick-questions { display: flex; flex-wrap: wrap; gap: 10px; justify-content: center; }
.quick-btn {
  padding: 10px 18px;
  border: 1px solid #ddd;
  border-radius: 999px;
  background: #fff;
  cursor: pointer;
  transition: all 0.2s;
}
.quick-btn:hover { border-color: #4f7cff; color: #4f7cff; }
.chat-message { display: flex; gap: 12px; max-width: 80%; }
.chat-message.user { align-self: flex-end; flex-direction: row-reverse; }
.chat-message.assistant { align-self: flex-start; }
.message-avatar {
  width: 40px; height: 40px;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 20px;
  background: rgba(79,124,255,0.1);
}
.message-content { display: flex; flex-direction: column; gap: 6px; }
.message-text {
  padding: 14px 18px;
  border-radius: 18px;
  line-height: 1.7;
  white-space: pre-wrap;
}
.chat-message.user .message-text {
  background: #4f7cff;
  color: #fff;
  border-bottom-right-radius: 4px;
}
.chat-message.assistant .message-text {
  background: #f8fbff;
  border: 1px solid #eee;
  border-bottom-left-radius: 4px;
}
.message-text.typing { animation: pulse 1.5s infinite; }
@keyframes pulse { 0%,100% { opacity: 1; } 50% { opacity: 0.5; } }
.message-meta { display: flex; gap: 8px; padding: 0 4px; }
.pill {
  display: inline-flex;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
}
.pill-template { background: rgba(245,158,11,0.12); color: #b45309; }
.pill-ai { background: rgba(79,124,255,0.12); color: #4f7cff; }
.chat-input-area { padding: 18px 24px; border-top: 1px solid #eee; background: #fff; }
.input-wrapper { display: flex; gap: 12px; align-items: flex-end; }
.input-wrapper textarea {
  flex: 1;
  padding: 13px 14px;
  border: 1px solid #ddd;
  border-radius: 15px;
  font: inherit;
  resize: none;
  max-height: 120px;
}
.send-btn {
  padding: 13px 24px;
  border: none;
  border-radius: 15px;
  background: #4f7cff;
  color: #fff;
  font-weight: 700;
  cursor: pointer;
}
.send-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.chat-error { margin-top: 8px; color: #dc2626; font-size: 13px; }
</style>
