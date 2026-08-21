<template>
  <div class="reviews-page">
    <h2>评论管理</h2>

    <div class="filter-bar">
      <select v-model="filterType">
        <option value="">全部评论</option>
        <option value="pending">待回复</option>
        <option value="replied">已回复</option>
      </select>
    </div>

    <div class="reviews-list">
      <div v-for="review in filteredReviews" :key="review.id" class="review-card">
        <div class="review-header">
          <span class="product-id">商品ID: {{ review.productId }}</span>
          <span class="rating">{{ '★'.repeat(review.rating) }}{{ '☆'.repeat(5 - review.rating) }}</span>
          <span class="time">{{ review.createTime }}</span>
        </div>
        <div class="review-content">{{ review.content }}</div>
        <div v-if="review.reply" class="review-reply">
          <strong>商家回复：</strong>{{ review.reply }}
        </div>
        <div class="review-actions">
          <button v-if="!review.reply" @click="openReplyDialog(review)" class="btn-reply">回复</button>
          <button @click="hideReview(review.id)" class="btn-hide">隐藏</button>
        </div>
      </div>
    </div>

    <!-- 回复弹窗 -->
    <div v-if="showReplyDialog" class="dialog-overlay" @click.self="showReplyDialog = false">
      <div class="dialog">
        <h3>回复评论</h3>
        <textarea v-model="replyContent" rows="4" placeholder="输入回复内容..."></textarea>
        <div class="dialog-actions">
          <button @click="showReplyDialog = false" class="btn-cancel">取消</button>
          <button @click="submitReply" class="btn-confirm">提交</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import http from '../../utils/http'

const reviews = ref([])
const filterType = ref('')              // 筛选：空=全部，pending=待回复，replied=已回复
const showReplyDialog = ref(false)
const replyContent = ref('')
const currentReview = ref(null)

// 计算属性：根据筛选类型过滤评论
const filteredReviews = computed(() => {
  if(!filterType.value) return reviews.value  // 无筛选的话返回全部评论
  if(filterType.value === 'pending')
    return reviews.value.filter(r => !r.reply)
  if(filterType.value === 'replied')
    return reviews.value.filter(r => r.reply)
  return reviews.value
})

// 提交回复的异步函数
const openReplyDialog = async (review) => {
  // 记录当前要回复的评论
  currentReview.value = review
  // 清空回复输入框
  replyContent.value = ''
  // 显示回复的弹窗
  showReplyDialog.value = true
}

// 隐藏品论的异步函数，也可以理解为软删除
const hideReview = async (id) => {
  const confirmed = window.confirm('确定要隐藏此评论吗？')
  if(!confirmed) return
  try {
    await http.put(`/merchant/reviews/${id}/hide`)
    // 在前端本地从列表中移除该评论
    reviews.value = reviews.value.filter(r => r.id !== id)
  } catch (error) {
    console.error('隐藏评论失败', error)
  }
}

// 提交回复的异步函数
const submitReply = async () => {
  // 如果回复内容是空白的话，直接返回，不发送请求
  if(!replyContent.value.trim) return
  try {
    await http.post('/merchant/reviews/reply',{
      reviewId: currentReview.value.id,  // 要回复的评论的id
      reply: replyContent.value  // 回复内容
    })
    // 本地更新评论对象的reply字段
    currentReview.value.reply = replyContent.value
    // 关闭回复的弹窗
    showReplyDialog.value = false
  } catch (error) {
    console.error('回复失败', error)
  }
}


</script>
