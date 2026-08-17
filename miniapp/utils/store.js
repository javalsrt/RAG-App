/** 本地存储工具 */
const KEYS = {
  TOKEN: 'token',
  USER_ID: 'userId',
  REAL_NAME: 'realName',
  USERNAME: 'username',
  ROLE: 'role'
}

export function setTokenInfo(data) {
  uni.setStorageSync(KEYS.TOKEN, data.token)
  uni.setStorageSync(KEYS.USER_ID, data.userId || data.id)
  uni.setStorageSync(KEYS.REAL_NAME, data.realName || '')
  uni.setStorageSync(KEYS.USERNAME, data.username || '')
  uni.setStorageSync(KEYS.ROLE, data.role || 1)
}

export function getToken() { return uni.getStorageSync(KEYS.TOKEN) || '' }
export function getUserId() { return uni.getStorageSync(KEYS.USER_ID) || 0 }
export function getRealName() { return uni.getStorageSync(KEYS.REAL_NAME) || '' }
export function getUsername() { return uni.getStorageSync(KEYS.USERNAME) || '' }
export function getRole() { return uni.getStorageSync(KEYS.ROLE) || 1 }

export function logout() {
  Object.values(KEYS).forEach(k => uni.removeStorageSync(k))
}
