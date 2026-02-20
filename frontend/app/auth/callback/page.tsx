"use client"

import { Suspense, useEffect } from "react"
import { useSearchParams } from "next/navigation"
import { apiClient } from "@/lib/api"

function CallbackInner() {
  const searchParams = useSearchParams()

  useEffect(() => {
    const handleOAuthCallback = async () => {
      const code = searchParams.get('code')
      const state = searchParams.get('state')

      // state에서 provider 정보 추출 (e.g. "provider=github")
      const provider = state?.includes('provider=')
        ? state.split('provider=')[1]
        : 'github' // Java backend only supports github

      if (!code) {
        window.opener?.postMessage({
          type: 'OAUTH2_ERROR',
          error: 'Missing authorization code in OAuth2 callback',
        }, window.location.origin)
        window.close()
        return
      }

      try {
        const origin = window.location.origin
        const basePath = process.env.NEXT_PUBLIC_BASE_PATH || ''
        const redirectUri = `${origin}${basePath}/auth/callback`

        // Exchange code for tokens — response is auto-unwrapped: { access_token, refresh_token }
        const tokenData = await apiClient.loginWithOAuth2(
          provider as 'github',
          code,
          redirectUri
        ) as any

        if (!tokenData?.access_token) {
          throw new Error('Login failed — no access token received')
        }

        // Store token temporarily so getCurrentUser() can use it
        localStorage.setItem('auth_token', tokenData.access_token)

        // Fetch user info with the new token
        const userData = await apiClient.getCurrentUser() as any

        const userInfo = {
          id: String(userData?.id || ''),
          email: userData?.email || '',
          name: userData?.name || '',
          provider: 'github' as const,
        }

        window.opener?.postMessage({
          type: 'OAUTH2_SUCCESS',
          user: userInfo,
          accessToken: tokenData.access_token,
        }, window.location.origin)
        window.close()
      } catch (error) {
        console.error('OAuth2 callback error:', error)
        window.opener?.postMessage({
          type: 'OAUTH2_ERROR',
          error: (error as Error).message || 'OAuth2 callback failed',
        }, window.location.origin)
        window.close()
      }
    }

    handleOAuthCallback()
  }, [searchParams])

  return (
    <div className="flex items-center justify-center min-h-screen bg-gray-100">
      <p>로그인 처리 중...</p>
    </div>
  )
}

export default function AuthCallbackPage() {
  return (
    <Suspense fallback={<div className="flex items-center justify-center min-h-screen bg-gray-100"><p>로그인 처리 중...</p></div>}>
      <CallbackInner />
    </Suspense>
  )
}
