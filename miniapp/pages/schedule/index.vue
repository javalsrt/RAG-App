<template>
<view class="schedule-page">
  <!-- 学期选择 -->
  <view class="semester-bar" @click="showSemesterPicker">
    <text class="semester-label">{{ currentSemester || '加载中...' }}</text>
    <text class="semester-arrow">▾</text>
  </view>

  <!-- 周选择器 -->
  <view class="week-bar">
    <button class="week-btn" @click="prevWeek">‹</button>
    <text class="week-label">第 {{ week }} 周</text>
    <button class="week-btn" @click="nextWeek">›</button>
  </view>

  <!-- 课表网格 -->
  <view class="grid">
    <view class="grid-header">
      <text class="header-cell">节次</text>
      <text class="header-cell">周一</text><text class="header-cell">周二</text><text class="header-cell">周三</text>
      <text class="header-cell">周四</text><text class="header-cell">周五</text><text class="header-cell">周六</text>
    </view>
    <view v-for="(row, ri) in gridData" :key="ri" class="grid-row">
      <text class="node-label">{{ nodes[ri] }}</text>
      <view v-for="(cell, ci) in row" :key="ci" class="grid-cell"
            :style="cell.hasCourse ? 'background:#0A84FF;color:#FFF;border-radius:4px;' : ''"
            @click="onCellClick(cell)">
        <text class="cell-course">{{ cell.name }}</text>
        <text class="cell-room">{{ cell.room }}</text>
      </view>
    </view>
  </view>

  <!-- 自定义底部导航栏 -->
  <tab-bar :current="1" />
</view>
</template>

<script>
import { getSchedule, getStudentSemesters } from '@/utils/api.js'

export default {
  data() {
    return {
      week: 1,
      maxWeek: 18,
      semesterList: [],
      currentSemester: null,
      // 艺术学部/汽车学部作息：每天8节课
      nodes: ['08:10\n08:50', '09:00\n09:40', '09:50\n10:30', '10:40\n11:20', '15:10\n15:50', '16:00\n16:40', '19:50\n20:10', '20:20\n21:00'],
      gridData: []
    }
  },
  onLoad() {
    this.initGrid()
    this.loadSemesters()
  },
  methods: {
    initGrid() {
      this.gridData = []
      // 8节课，6天（周一到周六，周日不排课）
      for (let n = 1; n <= 8; n++) {
        const row = []
        for (let d = 1; d <= 6; d++) {
          row.push({ day: d, node: n, hasCourse: false, name: '', room: '', detail: '' })
        }
        this.gridData.push(row)
      }
    },
    async loadSemesters() {
      try {
        const list = await getStudentSemesters() || []
        this.semesterList = list
        if (list.length > 0) {
          // 优先选当前学期（isCurrent），其次进行中的，最后选第一个
          let selected = list.find(s => s.isCurrent === true || s.isCurrent === 1) ||
                         list.find(s => s.status === 'ongoing') ||
                         list[0]
          this.currentSemester = selected.name
          this.maxWeek = selected.weekCount || 18
          this.calcCurrentWeek(selected.startDate)
          this.loadSchedule()
        }
      } catch (e) {
        // 失败兜底
        this.maxWeek = 18
        this.week = 1
        this.loadSchedule()
      }
    },
    calcCurrentWeek(startDate) {
      if (!startDate) { this.week = 1; return }
      const start = new Date(startDate.replace(/-/g, '/'))
      const now = new Date()
      const diffDays = Math.floor((now - start) / (1000 * 60 * 60 * 24))
      const week = Math.floor(diffDays / 7) + 1
      this.week = Math.max(1, Math.min(this.maxWeek, week))
    },
    showSemesterPicker() {
      if (this.semesterList.length === 0) return
      const names = this.semesterList.map(s => {
        let label = s.name
        if (s.status === 'ongoing') label += '（进行中）'
        else if (s.status === 'before') label += '（未开始）'
        else if (s.status === 'ended') label += '（已结束）'
        return label
      })
      const currentIdx = this.semesterList.findIndex(s => s.name === this.currentSemester)
      uni.showActionSheet({
        itemList: names,
        success: (res) => {
          const selected = this.semesterList[res.tapIndex]
          this.currentSemester = selected.name
          this.maxWeek = selected.weekCount || 18
          this.calcCurrentWeek(selected.startDate)
          this.loadSchedule()
        }
      })
    },
    prevWeek() { if (this.week > 1) { this.week--; this.loadSchedule() } },
    nextWeek() { if (this.week < this.maxWeek) { this.week++; this.loadSchedule() } },
    async loadSchedule() {
      try {
        const list = await getSchedule(this.week, this.currentSemester) || []
        this.initGrid()
        list.forEach(s => {
          const ri = s.startNode - 1
          const ci = s.dayOfWeek - 1
          if (ri >= 0 && ri < 8 && ci >= 0 && ci < 6) {
            const cell = this.gridData[ri][ci]
            cell.hasCourse = true
            cell.name = (s.courseName || '').substring(0, 6)
            cell.room = s.classroom || ''
            cell.detail = s.courseName + ' ' + (s.classroom || '') + ' ' + (s.startTime || '') + '-' + (s.endTime || '')
          }
        })
        this.gridData = [...this.gridData]
      } catch (e) {}
    },
    onCellClick(cell) {
      if (cell.detail) {
        uni.showModal({ title: '课程详情', content: cell.detail, showCancel: false })
      }
    }
  }
}
</script>

<style scoped>
.schedule-page { padding: 0 8px 72px; background: #F2F2F7; min-height: 100vh; }
.semester-bar { display: flex; align-items: center; justify-content: center; gap: 4px; padding: 12px 0 4px; }
.semester-label { font-size: 14px; font-weight: 500; color: #5E6AD2; }
.semester-arrow { font-size: 10px; color: #5E6AD2; }
.week-bar { display: flex; align-items: center; justify-content: center; padding: 8px 0 12px; gap: 16px; }
.week-btn { width: 36px; height: 36px; font-size: 20px; line-height: 36px; text-align: center; background: #FFF; border-radius: 50%; border: none; padding: 0; }
.week-btn::after { border: none; }
.week-label { font-size: 15px; font-weight: 600; color: #1D1D1F; }

.grid { background: #FFF; border-radius: 10px; overflow: hidden; }
.grid-header { display: flex; background: #F5F5F7; }
.header-cell { flex: 1; text-align: center; padding: 6px 2px; font-size: 11px; color: #86868B; font-weight: 500; }
.grid-row { display: flex; border-top: 1px solid #F0F0F0; }
.node-label { width: 44px; font-size: 9px; color: #86868B; text-align: center; padding: 4px 2px; white-space: pre-line; line-height: 1.3; flex-shrink: 0; }
.grid-cell { flex: 1; min-height: 48px; display: flex; flex-direction: column; align-items: center; justify-content: center; border-left: 1px solid #F0F0F0; padding: 2px; }
.cell-course { font-size: 10px; font-weight: 500; text-align: center; }
.cell-room { font-size: 8px; opacity: 0.8; text-align: center; }
</style>
