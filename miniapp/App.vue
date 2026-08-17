<script>
import { getToken, getUserId } from '@/utils/store.js'
import { connectWS } from '@/utils/ws.js'

export default {
  onLaunch() {
    uni.hideTabBar()
    const token = getToken()
    const userId = getUserId()
    if (token && userId) connectWS(userId)
  },
  onShow() {
    uni.hideTabBar()
    const token = getToken()
    const userId = getUserId()
    if (token && userId) connectWS(userId)
  }
}
</script>

<style>
/* iOS极简全局样式 */
page {
  background-color: #F2F2F7;
  font-family: -apple-system, 'PingFang SC', sans-serif;
  color: #1D1D1F;
  font-size: 14px;
}

.container { padding: 12px 16px 82px; }

.card {
  background: #FFFFFF;
  border-radius: 14px;
  padding: 16px;
  margin-bottom: 12px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}

.btn-primary {
  background: #0A84FF;
  color: #FFFFFF;
  border: none;
  border-radius: 8px;
  padding: 12px 24px;
  font-size: 15px;
  font-weight: 500;
  text-align: center;
  line-height: 1.4;
}

.btn-primary::after { border: none; }
.btn-primary:active { opacity: 0.85; }

.input-field {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid #E5E7EB;
  border-radius: 8px;
  font-size: 15px;
  background: #FFFFFF;
  box-sizing: border-box;
}

.text-subtle { color: #86868B; font-size: 12px; }
.text-title { font-size: 16px; font-weight: 600; color: #1D1D1F; }
.text-ink { color: #1D1D1F; }
</style>
