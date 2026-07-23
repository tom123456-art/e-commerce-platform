<template>
  <div class="home">
    <h1>欢迎来到电商平台</h1>
    <p>这是一个SpringBoot+Vue3前后端分离的项目</p>
    <div class="features">
      <div class="feature-card">
        <h3>🛍️商品预览</h3>
        <p>浏览热卖商品和个性化推荐</p>
      </div>
      <div class="feature-card">
        <h3>🛒购物车</h3>
        <p>加入购物车，一键结算</p>
      </div>
      <div class="feature-card">
        <h3>📦订单管理</h3>
        <p>查看订单状态，确认收货</p>
      </div>
      <div class="feature-card">
        <h3>🤖AI助手</h3>
        <p>智能问答、搜索、推荐</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.home {
  text-align: center;
  padding: 40px 0;
}

.home h1 {
  font-size: 32px;
  color: #333;
  margin-bottom: 10px;
}

.home p {
  color: #666;
  margin-bottom: 40px;
}

.features {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
}

.feature-card {
  background: #fff;
  border-radius: 8px;
  padding: 30px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.feature-card h3 {
  font-size: 20px;
  margin-bottom: 10px;
}

.feature-card p {
  color: #666;
}
</style>
