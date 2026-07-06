import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import { apiFetch } from '../api/client'

const TOKEN_KEY = 'keen-erp-token'
const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_KEY) || '')
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(Boolean(token))

  useEffect(() => {
    let active = true

    async function loadUser() {
      if (!token) {
        setLoading(false)
        setUser(null)
        return
      }

      setLoading(true)
      try {
        const profile = await apiFetch('/auth/me', { token })
        if (active) {
          setUser(profile)
        }
      } catch {
        if (active) {
          setToken('')
          setUser(null)
          localStorage.removeItem(TOKEN_KEY)
        }
      } finally {
        if (active) {
          setLoading(false)
        }
      }
    }

    loadUser()

    return () => {
      active = false
    }
  }, [token])

  const value = useMemo(() => {
    return {
      token,
      user,
      loading,
      async login(username, password) {
        const response = await apiFetch('/auth/login', {
          method: 'POST',
          body: { username, password }
        })
        localStorage.setItem(TOKEN_KEY, response.token)
        setToken(response.token)
        setUser(response.user)
        return response.user
      },
      logout() {
        localStorage.removeItem(TOKEN_KEY)
        setToken('')
        setUser(null)
      },
      hasPermission(permission) {
        if (!user) return false
        return user.permissions?.includes('admin:all') || user.permissions?.includes(permission)
      },
      hasAnyPermission(permissions = []) {
        if (!user) return false
        if (!permissions.length) return true
        return user.permissions?.includes('admin:all') || permissions.some((permission) => user.permissions?.includes(permission))
      }
    }
  }, [token, user, loading])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used inside AuthProvider')
  }
  return context
}
