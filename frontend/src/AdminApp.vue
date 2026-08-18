<template>
  <main v-if="isAdminLoginRoute" class="content-shell admin-auth-shell">
    <router-view />
  </main>
  <router-view v-else />
</template>

<script>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

export default {
  name: 'AdminApp',
  setup() {
    const route = useRoute()
    const isAdminLoginRoute = computed(() => Boolean(route.meta.adminLogin))

    return { isAdminLoginRoute }
  }
}
</script>

<style scoped>
.admin-auth-shell {
  min-height: 100vh;
}
</style>
