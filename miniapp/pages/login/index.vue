<template>
<view class="login-page">
  <view class="logo-area">
    <text class="logo">📚</text>
    <text class="app-name">学习中</text>
    <text class="subtitle">RAG智能学习助手</text>
  </view>

  <view class="form-card">
    <input class="input-field" v-model="username" placeholder="学号/工号" />
    <input class="input-field" v-model="password" placeholder="密码" password
           style="margin-top:12px" @confirm="doLogin" />
    <button class="btn-primary login-btn" :loading="loading" @click="doLogin">
      {{ loading ? '登录中...' : '登录' }}
    </button>
  </view>

  <text class="hint">测试账号: 20240101001 / 123456</text>
</view>
</template>

<script>
import { login } from '@/utils/api.js'
import { setTokenInfo } from '@/utils/store.js'

export default {
  data() {
    return { username: '', password: '', loading: false }
  },
  methods: {
    async doLogin() {
      if (!this.username || !this.password) {
        uni.showToast({ title: '请输入账号密码', icon: 'none' }); return
      }
      this.loading = true
      try {
        const res = await login(this.username, this.password)
        if (!res.token) {
          uni.showToast({ title: res.message || '登录失败', icon: 'none' })
          return
        }
        setTokenInfo({
          token: res.token, userId: res.userId,
          realName: res.realName, username: res.username, role: res.role
        })
        uni.reLaunch({ url: '/pages/index/index' })
      } catch (e) {
        uni.showToast({ title: '网络错误', icon: 'none' })
      } finally { this.loading = false }
    }
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 28px;
  background: #F2F2F7;
}
.logo-area { text-align: center; margin-bottom: 36px; }
.logo { font-size: 56px; }
.app-name { display: block; font-size: 26px; font-weight: 700; color: #1D1D1F; margin-top: 12px; }
.subtitle { display: block; font-size: 14px; color: #86868B; margin-top: 6px; }
.form-card { width: 100%; background: #FFF; border-radius: 14px; padding: 24px 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }
.login-btn { margin-top: 16px; }
.hint { margin-top: 24px; }
</style>
