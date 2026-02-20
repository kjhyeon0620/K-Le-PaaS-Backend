import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

/**
 * 시간을 "~ ago" 형식으로 변환 (한국 시간 기준)
 * 예: "2h ago", "3d ago", "just now"
 */
export function formatTimeAgo(dateString: string | null | undefined): string {
  if (!dateString) return 'N/A'

  try {
    // 한국 시간대 설정
    const koreaTimeZone = 'Asia/Seoul'
    
    // 현재 한국 시간
    const now = new Date()
    const koreaNow = new Date(now.toLocaleString("en-US", { timeZone: koreaTimeZone }))
    
    // 입력된 날짜를 한국 시간으로 변환
    const inputDate = new Date(dateString)
    const koreaInputDate = new Date(inputDate.toLocaleString("en-US", { timeZone: koreaTimeZone }))
    
    // 시간 차이 계산 (밀리초)
    const diffMs = koreaNow.getTime() - koreaInputDate.getTime()
    const seconds = Math.floor(diffMs / 1000)

    if (seconds < 60) return 'just now'

    const minutes = Math.floor(seconds / 60)
    if (minutes < 60) return `${minutes}m ago`

    const hours = Math.floor(minutes / 60)
    if (hours < 24) return `${hours}h ago`

    const days = Math.floor(hours / 24)
    if (days < 30) return `${days}d ago`

    const months = Math.floor(days / 30)
    if (months < 12) return `${months}mo ago`

    const years = Math.floor(months / 12)
    return `${years}y ago`
  } catch (error) {
    console.error('formatTimeAgo error:', error)
    return 'N/A'
  }
}

/**
 * 초 단위 시간을 "2m 7s" 형식으로 변환
 * 예: 127 → "2m 7s", 65 → "1m 5s", 45 → "45s"
 */
export function formatDuration(seconds: number | null | undefined): string {
  if (seconds === null || seconds === undefined) return 'N/A'
  if (seconds < 0) return 'N/A'

  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60

  if (mins === 0) {
    return `${secs}s`
  }

  return `${mins}m ${secs}s`
}
