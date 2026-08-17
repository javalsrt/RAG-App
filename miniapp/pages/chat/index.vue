<template>
  <view class="chat-page">
    <view class="chat-title">{{courseName}}</view>
    <scroll-view class="msg-list" scroll-y :scroll-into-view="toView" scroll-with-animation>
      <view v-if="loading" class="state-tip">加载中...</view>
      <view v-else-if="msgs.length === 0" class="state-tip">暂无消息，输入问题向AI提问</view>
      <view v-else>
        <view v-for="(item, idx) in msgs" :key="idx" :id="'msg-'+idx">
          <view v-if="item.type==='time'" class="time-sep">{{item.text}}</view>
          <view v-else class="msg-row" :class="item.senderRole==='student'?'mine':''">
            <view class="bubble" :class="item.senderRole==='student'?'bubble-mine':'bubble-other'">
              <view v-if="item.senderRole!=='student'" class="sender-name">
                {{item.senderRole==='ai'?'AI':item.senderName||'教师'}}
              </view>
              <view v-if="item.msgType==='text'" class="msg-text">{{item.displayContent}}</view>
              <image v-else-if="item.msgType==='image'" class="msg-img" :src="item.imageUrl" mode="widthFix" @click="previewImg(item.imageUrl)" />
            </view>
          </view>
        </view>
      </view>
    </scroll-view>
    <view class="input-bar">
      <button class="tool-btn" @click="pickImage">🖼</button>
      <input class="chat-input" placeholder="输入问题..." v-model="inputText" confirm-type="send" @confirm="sendText" :adjust-position="false" />
      <button class="send-btn" @click="sendText">发送</button>
    </view>
  </view>
</template>

<script>
const BASE = 'http://192.168.0.146:8080'
let timer = null
export default {
  data() {
    return { courseName: '', msgs: [], inputText: '', loading: true, toView: '' }
  },
  onLoad(options) {
    this.courseName = decodeURIComponent(options.courseName || '课程聊天')
    const pages = getCurrentPages()
    const page = pages[pages.length-1]
    if (page) {
      page.$vm = this
      page.$vm.courseName = this.courseName
    }
    this.load()
    this.markRead()
    timer = setInterval(() => this.load(), 5000)
  },
  onUnload() { if (timer) clearInterval(timer) },
  methods: {
    load() {
      const api = require('@/utils/api.js')
      api.request('/api/chat/' + encodeURIComponent(this.courseName)).then(data => {
        const msgs = (data || []).map(m => this.parseMsg(m))
        const items = this.buildItems(msgs)
        this.msgs = items
        this.loading = false
        this.toView = items.length ? 'msg-' + (items.length-1) : ''
      })
    },
    parseMsg(m) {
      const raw = m.content || ''
      const p = { ...m }
      if (raw.startsWith('[image]')) {
        p.msgType = 'image'; p.imageUrl = BASE + raw.substring(7); p.displayContent = '[图片]'
      } else if (raw.startsWith('[file]')) {
        p.msgType = 'file'
        const parts = raw.substring(6).split('|')
        p.fileName = parts[0] || '文件'; p.fileUrl = parts[1] || '#'
        p.displayContent = '📄 ' + p.fileName
      } else {
        p.msgType = 'text'; p.displayContent = raw
      }
      return p
    },
    buildItems(msgs) {
      const items = []; let prev = 0
      msgs.forEach(m => {
        const t = m.createdAt ? new Date(m.createdAt).getTime() : 0
        if (t && (!prev || t-prev > 10000)) {
          items.push({ type: 'time', text: this.fmtTime(m.createdAt) })
        }
        items.push({ type: 'msg', ...m })
        if (t) prev = t
      })
      return items
    },
    fmtTime(t) {
      if (!t) return ''
      const d = new Date(t)
      return [d.getHours(), d.getMinutes()].map(v => String(v).padStart(2,'0')).join(':')
    },
    markRead() {
      const api = require('@/utils/api.js')
      api.request('/api/chat/read', 'POST', { courseName: this.courseName })
    },
    sendText() {
      const text = this.inputText.trim()
      if (!text) return
      this.inputText = ''
      const api = require('@/utils/api.js')
      api.request('/api/chat/rag', 'POST', { courseName: this.courseName, content: text }).then(() => this.load())
    },
    pickImage() {
      uni.chooseImage({
        count: 1, sizeType: ['compressed'],
        success: (res) => {
          uni.uploadFile({
            url: BASE + '/api/chat/upload-file',
            filePath: res.tempFilePaths[0],
            name: 'file',
            formData: { courseName: this.courseName },
            header: { 'Authorization': 'Bearer ' + uni.getStorageSync('token') },
            success: (up) => {
              try {
                const data = JSON.parse(up.data)
                if (data.url) {
                  const url = '/uploads/chat/' + encodeURIComponent(this.courseName) + '/' + encodeURIComponent(data.fileName)
                  const api = require('@/utils/api.js')
                  api.request('/api/chat/send', 'POST', { courseName: this.courseName, content: '[image]'+url, senderRole:'student' }).then(() => this.load())
                }
              } catch (e) {}
            }
          })
        }
      })
    },
    previewImg(url) { uni.previewImage({ urls: [url] }) }
  }
}
</script>
<style scoped>
.chat-page { display:flex; flex-direction:column; height:100vh; background:#F2F2F7; }
.chat-title { font-size:14px; color:#86868B; text-align:center; padding:8px 0 6px; background:#F2F2F7; border-bottom:1px solid #E5E7EB; }
.msg-list { flex:1; padding:10px 14px; }
.state-tip { text-align:center; padding:60px 0; color:#86868B; font-size:13px; }
.time-sep { text-align:center; padding:12px 0 8px; font-size:11px; color:#C7C7CC; }
.msg-row { display:flex; margin-bottom:10px; }
.msg-row.mine { justify-content:flex-end; }
.bubble { display:inline-block; max-width:78%; padding:10px 14px; border-radius:16px; word-break:break-word; overflow-wrap:break-word; white-space:pre-wrap; }
.bubble-mine { background:#0A84FF; color:#FFF; border-bottom-right-radius:4px; }
.bubble-other { background:#FFF; color:#1D1D1F; border-bottom-left-radius:4px; box-shadow:0 1px 2px rgba(0,0,0,0.06); }
.sender-name { font-size:11px; font-weight:600; margin-bottom:4px; }
.bubble-other .sender-name { color:#0A84FF; }
.msg-text { font-size:16px; line-height:1.6; letter-spacing:0.3px; }
.msg-img { max-width:220px; border-radius:8px; margin-top:2px; }
.input-bar { display:flex; align-items:center; gap:6px; padding:8px 12px; background:#FFF; border-top:1px solid #E5E7EB; padding-bottom:calc(8px + env(safe-area-inset-bottom)); }
.tool-btn { width:36px; height:36px; font-size:17px; line-height:36px; text-align:center; background:#F5F5F7; border-radius:10px; border:none; padding:0; margin:0; flex-shrink:0; }
.tool-btn::after { border:none; }
.chat-input { flex:1; height:40px; padding:0 12px; font-size:15px; border:1px solid #E5E7EB; border-radius:20px; background:#FAFAFA; }
.send-btn { width:56px; height:36px; font-size:14px; font-weight:500; line-height:36px; text-align:center; border:none; border-radius:18px; background:#0A84FF; color:#FFF; padding:0; margin:0; flex-shrink:0; }
.send-btn::after { border:none; }
</style>
