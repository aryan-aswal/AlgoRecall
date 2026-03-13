"use client"

import React, { createContext, useContext, useState, useEffect, useCallback } from "react"
import { login as apiLogin, register as apiRegister, getProfile, clearToken } from "./api"

interface User {
  username: string
  email: string
}

interface AuthContextType {
  user: User | null
  loading: boolean
  login: (username: string, password: string) => Promise<void>
  register: (username: string, email: string, password: string, phoneNumber?: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextType>({
  user: null,
  loading: true,
  login: async () => {},
  register: async () => {},
  logout: () => {},
})

/** Sync the JWT to a cookie so Next.js middleware can read it for SSR route protection. */
function syncTokenCookie(token: string | null) {
  if (typeof document === "undefined") return
  if (token) {
    // Expires in 30 days; SameSite=Lax for Next.js middleware compatibility
    document.cookie = `auth_token=${token}; path=/; max-age=2592000; SameSite=Lax`
  } else {
    document.cookie = "auth_token=; path=/; max-age=0; SameSite=Lax"
  }
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const token = typeof window !== "undefined" ? localStorage.getItem("token") : null
    if (token) {
      syncTokenCookie(token)
      getProfile()
        .then(setUser)
        .catch(() => {
          clearToken()
          syncTokenCookie(null)
          setUser(null)
        })
        .finally(() => setLoading(false))
    } else {
      syncTokenCookie(null)
      setLoading(false)
    }
  }, [])

  const login = useCallback(async (username: string, password: string) => {
    const response = await apiLogin(username, password)
    // apiLogin already called setToken which stores under "token" key
    syncTokenCookie(localStorage.getItem("token"))
    setUser({ username, email: response.email ?? "" })
  }, [])

  const register = useCallback(
    async (username: string, email: string, password: string, phoneNumber?: string) => {
      await apiRegister(username, email, password, phoneNumber)
      // apiRegister already called setToken which stores under "token" key
      syncTokenCookie(localStorage.getItem("token"))
      const profile = await getProfile()
      setUser(profile)
    },
    []
  )

  const logout = useCallback(() => {
    clearToken()
    syncTokenCookie(null)
    setUser(null)
    if (typeof window !== "undefined") {
      window.location.href = "/login"
    }
  }, [])

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
