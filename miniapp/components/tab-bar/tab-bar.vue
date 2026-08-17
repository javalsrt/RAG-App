<template>
<view class="tab-bar">
  <view
    v-for="(item, index) in list"
    :key="index"
    class="tab-item"
    :class="{ active: current === index }"
    @click="switchTab(index)"
  >
    <image class="tab-icon" :src="current === index ? item.iconActive : item.icon" mode="aspectFit" />
    <text class="tab-text" :class="{ active: current === index }">{{ item.text }}</text>
  </view>
</view>
</template>

<script>
export default {
  props: {
    current: { type: Number, default: 0 }
  },
  data() {
    return {
      list: [
        {
          text: '学习',
          icon: '/static/tabbar/learn.svg',
          iconActive: '/static/tabbar/learn-active.svg',
          path: '/pages/index/index'
        },
        {
          text: '课表',
          icon: '/static/tabbar/schedule.svg',
          iconActive: '/static/tabbar/schedule-active.svg',
          path: '/pages/schedule/index'
        },
        {
          text: '我的',
          icon: '/static/tabbar/profile.svg',
          iconActive: '/static/tabbar/profile-active.svg',
          path: '/pages/profile/index'
        }
      ]
    }
  },
  methods: {
    switchTab(index) {
      if (this.current === index) return
      uni.switchTab({ url: this.list[index].path })
    }
  }
}
</script>

<style scoped>
.tab-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 56px;
  background: #FFFFFF;
  display: flex;
  align-items: center;
  box-shadow: 0 -1px 0 rgba(0, 0, 0, 0.06);
  padding-bottom: env(safe-area-inset-bottom);
  z-index: 999;
}

.tab-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 6px 0;
}

.tab-icon {
  width: 24px;
  height: 24px;
  margin-bottom: 2px;
}

.tab-text {
  font-size: 10px;
  color: #86868B;
  transition: color 0.2s;
}

.tab-text.active {
  color: #0A84FF;
  font-weight: 600;
}

.tab-item.active {
  transform: scale(1.05);
  transition: transform 0.2s;
}
</style>
