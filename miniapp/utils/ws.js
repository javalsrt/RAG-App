let socketTask = null
let pingTimer = null
let listeners = []
let connecting = false

const WS_BASE = 'ws://192.168.0.146:8080'

/** 连接 WebSocket（防止重复连接） */
export function connectWS(userId) {
  if (socketTask || connecting) return
  connecting = true

  socketTask = uni.connectSocket({
    url: WS_BASE + '/ws/schedule?userId=' + userId + '&role=student',
    success: () => {},
    fail: () => { connecting = false; socketTask = null }
  })

  socketTask.onOpen(() => {
    console.log('WS connected')
    connecting = false
    pingTimer = setInterval(() => {
      try { if (socketTask) socketTask.send({ data: 'ping' }) } catch (e) {}
    }, 30000)
  })

  socketTask.onMessage(res => {
    try {
      const msg = JSON.parse(res.data)
      if (msg.type === 'chat_update') {
        listeners.forEach(l => l(msg.data))
      }
    } catch (e) {}
  })

  socketTask.onClose(() => {
    socketTask = null
    connecting = false
    if (pingTimer) { clearInterval(pingTimer); pingTimer = null }
  })

  socketTask.onError(() => {
    socketTask = null
    connecting = false
    if (pingTimer) { clearInterval(pingTimer); pingTimer = null }
  })
}

/** 添加消息监听 */
export function onChatUpdate(listener) {
  listeners.push(listener)
}

/** 移除消息监听 */
export function offChatUpdate(listener) {
  const i = listeners.indexOf(listener)
  if (i >= 0) listeners.splice(i, 1)
}

/** 断开 */
export function disconnectWS() {
  try { if (socketTask) socketTask.close() } catch (e) {}
  socketTask = null
  connecting = false
  if (pingTimer) { clearInterval(pingTimer); pingTimer = null }
  listeners = []
}
