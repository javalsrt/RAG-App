<template>
<view class="container profile-page">
  <!-- 头部 -->
  <view class="profile-header">
    <text class="avatar">{{ avatarChar }}</text>
    <text class="name">{{ realName }}</text>
    <text class="info">学号: {{ username }}</text>
  </view>

  <!-- 统计卡片 -->
  <view class="stats-card">
    <view class="stat-item">
      <text class="stat-value">{{ focusMinutes }}</text>
      <text class="stat-label">专注(分)</text>
    </view>
    <view class="stat-divider" />
    <view class="stat-item">
      <text class="stat-value">{{ quizCount }}</text>
      <text class="stat-label">测评</text>
    </view>
  </view>

  <!-- 功能入口 -->
  <view class="section-title">功能</view>
  <view class="menu-card">
    <view class="menu-item" @click="goChat">
      <text class="menu-icon">💬</text><text class="menu-text">课程聊天</text><text class="menu-arrow">›</text>
    </view>
    <view class="menu-item" @click="goSchedule">
      <text class="menu-icon">📅</text><text class="menu-text">查看课表</text><text class="menu-arrow">›</text>
    </view>
  </view>

  <!-- 退出 -->
  <button class="btn-logout" @click="doLogout">退出登录</button>

  <!-- 自定义底部导航栏 -->
  <tab-bar :current="2" />
</view>
</template>

<script>
import { getRealName, getUsername, logout } from '@/utils/store.js'
import { disconnectWS } from '@/utils/ws.js'
import { request } from '@/utils/api.js'

export default {
  data() { return { realName: '', username: '', focusMinutes: 0, quizCount: 0 } },
  computed: {
    avatarChar() { return (this.realName || '学').charAt(0) }
  },
  onShow() {
    this.realName = getRealName()
    this.username = getUsername()
    this.loadStats()
  },
  methods: {
    async loadStats() {
      try {
        const [focus, quiz] = await Promise.all([
          request('/api/focus/total'),
          request('/api/quiz/count')
        ])
        if (focus && focus.totalSeconds) this.focusMinutes = Math.round(focus.totalSeconds / 60)
        if (quiz && quiz.count != null) this.quizCount = quiz.count
      } catch (e) {}
    },
    goChat() { uni.switchTab({ url: '/pages/index/index' }) },
    goSchedule() { uni.switchTab({ url: '/pages/schedule/index' }) },
    doLogout() {
      disconnectWS()
      logout()
      uni.reLaunch({ url: '/pages/login/index' })
    }
  }
}
</script>

<style scoped>
.profile-page { padding-top: 16px; }

.profile-header { text-align: center; padding: 24px 0 16px; }
.avatar { width: 68px; height: 68px; line-height: 68px; text-align: center; font-size: 28px; font-weight: 600; color: #FFF; background: #0A84FF; border-radius: 50%; display: inline-block; }
.name { display: block; font-size: 20px; font-weight: 600; color: #1D1D1F; margin-top: 12px; }
.info { display: block; font-size: 14px; color: #86868B; margin-top: 4px; }

.stats-card {
  background: #FFF; border-radius: 14px; padding: 20px;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  margin-top: 16px;
}
.stat-item { flex: 1; text-align: center; }
.stat-value { font-size: 24px; font-weight: 600; color: #0A84FF; }
.stat-label { display: block; font-size: 12px; color: #86868B; margin-top: 4px; }
.stat-divider { width: 1px; height: 36px; background: #E5E7EB; }

.section-title { font-size: 13px; font-weight: 600; color: #86868B; margin: 20px 0 8px; }

.menu-card { background: #FFF; border-radius: 14px; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }
.menu-item { display: flex; align-items: center; padding: 14px 16px; border-bottom: 1px solid #F0F0F0; }
.menu-item:last-child { border-bottom: none; }
.menu-icon { font-size: 20px; width: 32px; }
.menu-text { flex: 1; font-size: 15px; color: #1D1D1F; }
.menu-arrow { font-size: 18px; color: #C7C7CC; }

.btn-logout {
  margin-top: 24px; padding: 12px; text-align: center;
  font-size: 15px; color: #FF3B30; background: #FFF;
  border-radius: 10px; border: none;
}
.btn-logout::after { border: none; }
</style>
