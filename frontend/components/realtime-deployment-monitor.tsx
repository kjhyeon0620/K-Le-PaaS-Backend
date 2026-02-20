"use client"

import { useEffect, useState } from "react"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Progress } from "@/components/ui/progress"
import { CheckCircle, XCircle, Clock, Zap, Wifi, WifiOff, RefreshCw } from "lucide-react"
import { useGlobalWebSocket } from "@/hooks/use-global-websocket"
import { DeploymentProgress } from "./deployment-progress"
import { apiClient } from "@/lib/api"

interface RealtimeDeploymentMonitorProps {
  deploymentId: string | number
  repository: string
  initialStatus: "running" | "success" | "failed" | "completed" | "pending" | "unknown" | "cancelled"
  initialStages?: {
    sourcecommit: {
      status: "success" | "failed" | null
      duration: number | null
    }
    sourcebuild: {
      status: "success" | "failed" | null
      duration: number | null
    }
    sourcedeploy: {
      status: "success" | "failed" | null
      duration: number | null
    }
  }
  initialTiming?: {
    started_at: string
    completed_at: string | null
    total_duration: number | null
  }
  error?: {
    message: string | null
    stage: string | null
  } | null
  auto_deploy_enabled: boolean
  userId?: string
}

interface WebSocketMessage {
  type: string
  deployment_id?: string
  user_id?: string
  stage?: string
  status?: string
  progress?: number
  elapsed_time?: number
  message?: string
  started_at?: string
  completed_at?: string
  duration?: number
  total_duration?: number
  data?: any
  timestamp?: string
}

export function RealtimeDeploymentMonitor({
  deploymentId,
  repository,
  initialStatus,
  initialStages,
  initialTiming,
  error,
  auto_deploy_enabled,
  userId
}: RealtimeDeploymentMonitorProps) {
  const [currentStatus, setCurrentStatus] = useState(initialStatus)
  const [expanded, setExpanded] = useState(initialStatus === "running")
  // ✅ initialStages를 깊은 복사하여 state 초기화 (스냅샷 재생 전에도 기존 데이터 보존)
  const [currentStages, setCurrentStages] = useState(() => {
    if (initialStages) {
      return {
        sourcecommit: { ...initialStages.sourcecommit },
        sourcebuild: { ...initialStages.sourcebuild },
        sourcedeploy: { ...initialStages.sourcedeploy }
      }
    }
    return {
      sourcecommit: { status: null, duration: null },
      sourcebuild: { status: null, duration: null },
      sourcedeploy: { status: null, duration: null }
    }
  })
  const [currentTiming, setCurrentTiming] = useState(initialTiming || {
    started_at: new Date().toISOString(),
    completed_at: null,
    total_duration: null
  })
  const [currentError, setCurrentError] = useState(error)
  const [lastUpdate, setLastUpdate] = useState<Date | null>(null)
  const [runningElapsedSec, setRunningElapsedSec] = useState<number>(0)

  // ✅ WebSocket이 작동하지 않을 때 대비해서 폴링으로 최신 상태 가져오기
  useEffect(() => {
    if (currentStatus !== "running") return // 실행 중일 때만 폴링

    const interval = setInterval(async () => {
      try {
        // ✅ apiClient를 사용하여 백엔드로 요청 (자동으로 8000 포트로 프록시됨)
        const response = await apiClient.getDeploymentHistory(
          typeof deploymentId === 'string' ? parseInt(deploymentId, 10) : deploymentId
        ) as any

        // deployment 객체 내부에 stages가 있음
        const data = response.deployment || response

        if (data.stages) {
            // ✅ API 응답의 "pending" 상태를 null로 변환
            const normalizeStageStatus = (status: any) => {
              if (status === "pending" || status === "waiting") return null
              if (status === "success" || status === "failed") return status
              return null
            }

            setCurrentStages(prev => {
              const normalized = {
                sourcecommit: {
                  ...prev.sourcecommit,  // ✅ WebSocket 데이터 유지 (progress, started_at, elapsed_time 등)
                  ...(data.stages.sourcecommit || {}),  // API 응답으로 덮어쓰기
                  status: normalizeStageStatus(data.stages.sourcecommit?.status)
                },
                sourcebuild: {
                  ...prev.sourcebuild,  // ✅ WebSocket 데이터 유지
                  ...(data.stages.sourcebuild || {}),
                  status: normalizeStageStatus(data.stages.sourcebuild?.status)
                },
                sourcedeploy: {
                  ...prev.sourcedeploy,  // ✅ WebSocket 데이터 유지
                  ...(data.stages.sourcedeploy || {}),
                  status: normalizeStageStatus(data.stages.sourcedeploy?.status)
                }
              }
              return normalized
            })
          }
          if (data.status && data.status !== currentStatus) {
            setCurrentStatus(data.status)
          }
      } catch (error) {
        console.error("Failed to poll deployment status:", error)
      }
    }, 3000) // 3초마다 폴링

    return () => clearInterval(interval)
  }, [deploymentId, currentStatus])

  const { isConnected, connectionStatus, sendMessage } = useGlobalWebSocket({
    deploymentId: typeof deploymentId === 'string' ? parseInt(deploymentId, 10) : deploymentId,
    userId: userId, // userId 전달하여 broadcast_to_user가 작동하도록 함
    onMessage: (message: WebSocketMessage) => {
      // deploymentId가 일치하는 메시지만 처리 (타입 불일치 고려)
      if (message.deployment_id) {
        const messageDeploymentId = String(message.deployment_id)
        const currentDeploymentId = String(deploymentId)

        if (messageDeploymentId !== currentDeploymentId) {
          return
        }
      }
      
      setLastUpdate(new Date())

      switch (message.type) {
        case "deployment_started":
          setCurrentStatus("running")
          setCurrentError(null)
          if (message.timestamp || message.started_at) {
            setCurrentTiming(prev => ({
              ...prev,
              started_at: message.started_at || message.timestamp || prev.started_at
            }))
          }
          break

        case "stage_started":
          if (message.stage) {
            setCurrentStages(prev => ({
              ...prev,
              [message.stage!]: {
                ...prev[message.stage as keyof typeof prev],
                status: null,
                progress: 0,
                elapsed_time: 0,
                message: message.message,
                started_at: message.started_at || message.timestamp
              }
            }))
          }
          break

        case "stage_progress":
          if (message.stage && typeof message.progress === "number") {
            setCurrentStages(prev => {
              const prevStage = prev[message.stage as keyof typeof prev]
              // ✅ progress가 100이면 자동으로 success 상태로 전환 (스냅샷 재생 시 stage_completed가 누락될 수 있음)
              const isCompleted = message.progress === 100
              const updated = {
                ...prev,
                [message.stage!]: {
                  ...prevStage,
                  status: isCompleted ? "success" : (prevStage?.status || null),
                  progress: message.progress,
                  elapsed_time: message.elapsed_time,
                  message: message.message,
                  // 새로고침 후 스냅샷 재생 시 서버에서 온 started_at로 초기화
                  started_at: message.started_at || (prevStage as any)?.started_at,
                  // progress 100이면 completed_at도 설정
                  completed_at: isCompleted ? (message.timestamp || new Date().toISOString()) : (prevStage as any)?.completed_at
                }
              }
              return updated
            })
          }
          break

        case "stage_completed":
          if (message.stage && message.status) {
            setCurrentStages(prev => ({
              ...prev,
              [message.stage!]: {
                ...prev[message.stage as keyof typeof prev],
                status: message.status as "success" | "failed",
                duration: (message.duration ?? message.data?.duration ?? null),
                completed_at: message.completed_at || message.timestamp,
                progress: 100 // 완료 시 100%로 설정
              }
            }))

            // If any stage failed, update overall status
            if (message.status === "failed") {
              setCurrentStatus("failed")
              setCurrentError({
                message: message.data?.message || "Stage failed",
                stage: message.stage
              })
            }
          }
          break

        case "deployment_completed":
          setCurrentStatus(message.status as "success" | "failed")
          setLastUpdate(new Date()) // 완료 시 lastUpdate 설정
          if (message.data?.total_duration) {
            setCurrentTiming(prev => ({
              ...prev,
              completed_at: message.completed_at || new Date().toISOString(),
              total_duration: message.total_duration ?? message.data.total_duration
            }))
          }
          break

        case "connection_established":
          console.log("WebSocket connection established")
          break

        case "pong":
          // pong 메시지는 연결 상태 확인용이므로 무시
          break

        default:
          console.log("Unknown message type:", message.type)
      }
    },
    onError: (error) => {
      console.error("WebSocket error:", error)
    },
    onClose: (event) => {
      console.log("WebSocket closed:", event.code, event.reason)
    }
  })

  // format helpers
  const formatDuration = (sec?: number | null) => {
    if (!sec || sec < 0) return "-"
    const h = Math.floor(sec / 3600)
    const m = Math.floor((sec % 3600) / 60)
    const s = Math.floor(sec % 60)
    if (h > 0) return `${h}h ${m}m ${s}s`
    if (m > 0) return `${m}m ${s}s`
    return `${s}s`
  }

  const formatKST = (iso?: string | null) => {
    if (!iso) return "-"
    try {
      // 강건한 파싱: 타임존 표기가 없으면 UTC로 간주하여 'Z'를 붙여 파싱
      const hasTz = /Z|[+\-]\d{2}:?\d{2}$/.test(iso)
      const normalized = hasTz ? iso : `${iso.replace(' ', 'T')}Z`
      return new Date(normalized).toLocaleTimeString('ko-KR', { timeZone: 'Asia/Seoul' })
    } catch {
      return "-"
    }
  }

  // update elapsed while running
  useEffect(() => {
    if (currentStatus !== "running" || !currentTiming?.started_at) return
    const started = new Date(currentTiming.started_at).getTime()
    const tick = () => setRunningElapsedSec(Math.max(0, Math.floor((Date.now() - started) / 1000)))
    tick()
    const id = setInterval(tick, 1000)
    return () => clearInterval(id)
  }, [currentStatus, currentTiming?.started_at])

  const getConnectionStatusBadge = () => {
    switch (connectionStatus) {
      case "connecting":
        return (
          <Badge variant="outline" className="bg-yellow-100 text-yellow-800">
            <RefreshCw className="w-3 h-3 mr-1 animate-spin" />
            Connecting
          </Badge>
        )
      case "connected":
        return (
          <Badge variant="outline" className="bg-green-100 text-green-800">
            <Wifi className="w-3 h-3 mr-1" />
            Connected
          </Badge>
        )
      case "disconnected":
        return (
          <Badge variant="outline" className="bg-gray-100 text-gray-800">
            <WifiOff className="w-3 h-3 mr-1" />
            Disconnected
          </Badge>
        )
      case "error":
        return (
          <Badge variant="outline" className="bg-red-100 text-red-800">
            <WifiOff className="w-3 h-3 mr-1" />
            Error
          </Badge>
        )
      default:
        return null
    }
  }

  return (
    <div className="space-y-4">
      {/* Single card: header (Connected + Show details) + optional details */}
      <Card>
        <CardHeader>
          <div
            className="flex items-center justify-between cursor-pointer select-none"
            onClick={() => setExpanded(v => !v)}
            role="button"
            aria-expanded={expanded}
            aria-controls={`deployment-details-${deploymentId}`}
            title={expanded ? 'Click to hide details' : 'Click to show details'}
          >
            <div>
              <CardTitle className="text-lg">Real-time Deployment Monitor</CardTitle>
              <CardDescription>
                Live updates for deployment #{deploymentId} • {repository}
              </CardDescription>
              {/* inline summary from existing details */}
              <div className="mt-2 flex flex-wrap items-center gap-3 text-xs text-muted-foreground">
                <span>Started: {formatKST(currentTiming?.started_at)}</span>
                <span>Completed: {currentTiming?.completed_at ? formatKST(currentTiming.completed_at) : '-'}</span>
                <span>Duration: {currentStatus === 'running' ? formatDuration(runningElapsedSec) : formatDuration(currentTiming?.total_duration ?? 0)}</span>
                {auto_deploy_enabled && (
                  <Badge variant="outline" className="bg-green-50 text-green-700 border-green-200">Auto Deploy</Badge>
                )}
              </div>
            </div>
            <div className="flex items-center gap-2">
              {getConnectionStatusBadge()}
              {!isConnected && (
                <Button size="sm" variant="outline" onClick={() => window.location.reload()}>
                  <RefreshCw className="w-4 h-4 mr-1" />
                  Reconnect
                </Button>
              )}
              <Badge variant="outline" className={currentStatus === 'running' ? 'bg-blue-50 text-blue-700' : currentStatus === 'success' ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-700'}>
                {currentStatus === 'running' ? 'Running' : currentStatus === 'success' ? 'Success' : currentStatus}
              </Badge>
              <Button size="sm" variant="outline" onClick={(e) => { e.stopPropagation(); setExpanded(v => !v) }}>
                {expanded ? 'Hide details' : 'Show details'}
              </Button>
            </div>
          </div>
        </CardHeader>
        {expanded && (
          <CardContent id={`deployment-details-${deploymentId}`}>
            <div className="text-sm text-muted-foreground mb-4">
              {currentStatus === "success" && (
                <div className="text-green-600">✅ Deployment completed successfully</div>
              )}
              {currentStatus === "failed" && (
                <div className="text-red-600">❌ Deployment failed</div>
              )}
              {currentStatus === "running" && !lastUpdate && (
                <div className="text-blue-600">🔄 Deployment in progress...</div>
              )}
              {currentStatus === "running" && lastUpdate && (
                <div className="text-blue-600">🔄 Deployment in progress... (Last update: {lastUpdate.toLocaleTimeString()})</div>
              )}
              {!currentStatus && (
                <div className="text-gray-600">⏳ Initializing deployment...</div>
              )}
            </div>
            <DeploymentProgress
              deploymentId={deploymentId}
              repository={repository}
              status={currentStatus}
              stages={currentStages}
              timing={currentTiming}
              error={currentError}
              auto_deploy_enabled={auto_deploy_enabled}
            />
          </CardContent>
        )}
      </Card>
    </div>
  )
}
