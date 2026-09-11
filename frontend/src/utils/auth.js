const TOKEN_KEY = 'elective_token'
const USER_KEY = 'elective_user'

/**
 * 令牌与用户信息的本地持久化。
 * 使用 sessionStorage 可在关闭标签页后自动失效，降低令牌泄露风险。
 */
export function getToken() {
  return sessionStorage.getItem(TOKEN_KEY) || localStorage.getItem(TOKEN_KEY)
}

export function setToken(token, remember = false) {
  if (remember) {
    localStorage.setItem(TOKEN_KEY, token)
  } else {
    sessionStorage.setItem(TOKEN_KEY, token)
  }
}

export function removeToken() {
  sessionStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(TOKEN_KEY)
}

export function getStoredUser() {
  const raw = sessionStorage.getItem(USER_KEY) || localStorage.getItem(USER_KEY)
  try {
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export function setStoredUser(user, remember = false) {
  const value = JSON.stringify(user)
  if (remember) {
    localStorage.setItem(USER_KEY, value)
  } else {
    sessionStorage.setItem(USER_KEY, value)
  }
}

export function removeStoredUser() {
  sessionStorage.removeItem(USER_KEY)
  localStorage.removeItem(USER_KEY)
}
