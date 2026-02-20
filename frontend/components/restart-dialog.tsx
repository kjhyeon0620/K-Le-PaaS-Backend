"use client"

import { useState } from "react"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Play, AlertTriangle, RefreshCw, GitCommit } from "lucide-react"
import { api } from "@/lib/api"
import { useToast } from "@/hooks/use-toast"

interface RestartDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  owner: string
  repo: string
  currentCommitSha?: string
  currentImage?: string
  onRestartSuccess?: () => void
}

export function RestartDialog({
  open,
  onOpenChange,
  owner,
  repo,
  currentCommitSha,
  currentImage,
  onRestartSuccess,
}: RestartDialogProps) {
  const [restarting, setRestarting] = useState(false)
  const { toast } = useToast()

  const handleRestart = async () => {
    try {
      setRestarting(true)
      await api.restartDeployment(owner, repo)

      toast({
        title: "재시작 시작",
        description: `${owner}/${repo} 배포를 재시작합니다.`,
      })

      onOpenChange(false)
      onRestartSuccess?.()
    } catch (error) {
      console.error("Restart failed:", error)
      toast({
        title: "재시작 실패",
        description: String(error),
        variant: "destructive",
      })
    } finally {
      setRestarting(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <RefreshCw className="w-5 h-5" />
            배포 재시작 - {owner}/{repo}
          </DialogTitle>
          <DialogDescription>
            현재 이미지로 배포를 다시 시작합니다.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-4">
          {/* Current Deployment Info */}
          <div className="p-4 bg-muted rounded-lg space-y-3">
            <div>
              <p className="text-sm font-semibold mb-1">현재 배포 정보</p>
            </div>

            {currentCommitSha && (
              <div className="flex items-center gap-2">
                <GitCommit className="w-5 h-5 text-muted-foreground" />
                <span className="text-sm text-muted-foreground">커밋:</span>
                <code className="font-mono text-sm bg-background px-2 py-1 rounded">
                  {currentCommitSha.slice(0, 7)}
                </code>
              </div>
            )}
          </div>

          {/* Warning Info */}
          <div className="p-3 bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-800 rounded-lg">
            <div className="flex items-start gap-2">
              <AlertTriangle className="w-4 h-4 text-yellow-600 mt-0.5" />
              <div className="text-sm">
                <p className="font-semibold text-yellow-900 dark:text-yellow-100 mb-1">안내</p>
                <ul className="text-yellow-700 dark:text-yellow-300 space-y-1 list-disc list-inside">
                  <li>현재 실행 중인 Pod가 종료되고 새로운 Pod가 시작됩니다</li>
                  <li>짧은 다운타임이 발생할 수 있습니다</li>
                  <li>이미지나 설정은 변경되지 않습니다</li>
                </ul>
              </div>
            </div>
          </div>

          {/* What Happens */}
          <div className="text-xs text-muted-foreground space-y-1">
            <p className="font-semibold mb-2">재시작 과정:</p>
            <ol className="list-decimal list-inside space-y-1">
              <li>현재 실행 중인 Pod 종료</li>
              <li>동일한 이미지로 새 Pod 생성</li>
              <li>헬스 체크 통과 후 트래픽 전환</li>
              <li>롤링 업데이트 방식으로 진행</li>
            </ol>
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={restarting}>
            취소
          </Button>
          <Button onClick={handleRestart} disabled={restarting}>
            {restarting ? (
              <>
                <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
                재시작 중...
              </>
            ) : (
              <>
                <Play className="w-4 h-4 mr-2" />
                재시작 실행
              </>
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
