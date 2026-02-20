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
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Slider } from "@/components/ui/slider"
import { Badge } from "@/components/ui/badge"
import { Scale, AlertCircle, TrendingUp, TrendingDown } from "lucide-react"
import { api } from "@/lib/api"
import { useToast } from "@/hooks/use-toast"

interface ScaleDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  owner: string
  repo: string
  currentReplicas?: number
  onScaleSuccess?: () => void
}

export function ScaleDialog({
  open,
  onOpenChange,
  owner,
  repo,
  currentReplicas = 1,
  onScaleSuccess,
}: ScaleDialogProps) {
  const [targetReplicas, setTargetReplicas] = useState(currentReplicas)
  const [scaling, setScaling] = useState(false)
  const { toast } = useToast()

  const handleScaleChange = (value: number[]) => {
    setTargetReplicas(value[0])
  }

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = parseInt(e.target.value, 10)
    if (!isNaN(value) && value >= 0 && value <= 10) {
      setTargetReplicas(value)
    }
  }

  const handleScale = async () => {
    if (targetReplicas === currentReplicas) {
      toast({
        title: "변경사항 없음",
        description: "현재 레플리카 개수와 동일합니다.",
        variant: "destructive",
      })
      return
    }

    try {
      setScaling(true)
      await api.scaleDeployment(owner, repo, targetReplicas)

      toast({
        title: "스케일링 시작",
        description: `레플리카를 ${currentReplicas}개에서 ${targetReplicas}개로 조정합니다.`,
      })

      onOpenChange(false)
      onScaleSuccess?.()
    } catch (error) {
      console.error("Scaling failed:", error)
      toast({
        title: "스케일링 실패",
        description: String(error),
        variant: "destructive",
      })
    } finally {
      setScaling(false)
    }
  }

  const getScaleDirection = () => {
    if (targetReplicas > currentReplicas) {
      return { icon: TrendingUp, text: "Scale Up", color: "text-green-600" }
    } else if (targetReplicas < currentReplicas) {
      return { icon: TrendingDown, text: "Scale Down", color: "text-orange-600" }
    }
    return { icon: Scale, text: "No Change", color: "text-gray-600" }
  }

  const scaleDirection = getScaleDirection()
  const ScaleIcon = scaleDirection.icon

  // Reset target when opening
  const handleOpenChange = (newOpen: boolean) => {
    if (newOpen) {
      setTargetReplicas(currentReplicas)
    }
    onOpenChange(newOpen)
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Scale className="w-5 h-5" />
            배포 스케일링 - {owner}/{repo}
          </DialogTitle>
          <DialogDescription>
            Pod 레플리카 개수를 조정합니다. (0-10개)
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-6 py-4">
          {/* Current Status */}
          <div className="p-4 bg-muted rounded-lg">
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm font-semibold">현재 레플리카</span>
              <Badge variant="outline" className="text-lg font-mono">
                {currentReplicas}개
              </Badge>
            </div>
          </div>

          {/* Target Replicas Slider */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <Label htmlFor="replicas-slider" className="text-sm font-semibold">
                목표 레플리카
              </Label>
              <div className="flex items-center gap-2">
                <Input
                  id="replicas-input"
                  type="number"
                  min={0}
                  max={10}
                  value={targetReplicas}
                  onChange={handleInputChange}
                  className="w-20 h-8 text-center font-mono"
                />
                <span className="text-sm text-muted-foreground">개</span>
              </div>
            </div>

            <Slider
              id="replicas-slider"
              min={0}
              max={10}
              step={1}
              value={[targetReplicas]}
              onValueChange={handleScaleChange}
              className="w-full"
            />

            <div className="flex justify-between text-xs text-muted-foreground">
              <span>0</span>
              <span>5</span>
              <span>10</span>
            </div>
          </div>

          {/* Scale Direction Indicator */}
          {targetReplicas !== currentReplicas && (
            <div className={`p-4 rounded-lg border ${
              targetReplicas > currentReplicas
                ? "bg-green-50 border-green-200 dark:bg-green-900/20 dark:border-green-800"
                : "bg-orange-50 border-orange-200 dark:bg-orange-900/20 dark:border-orange-800"
            }`}>
              <div className="flex items-center gap-3">
                <ScaleIcon className={`w-5 h-5 ${scaleDirection.color}`} />
                <div className="flex-1">
                  <p className={`text-sm font-semibold ${scaleDirection.color}`}>
                    {scaleDirection.text}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    {currentReplicas}개 → {targetReplicas}개
                    {targetReplicas > currentReplicas ? " (+)" : " (-)"}
                    {Math.abs(targetReplicas - currentReplicas)}개
                  </p>
                </div>
              </div>
            </div>
          )}

          {/* Warning for zero replicas */}
          {targetReplicas === 0 && (
            <div className="p-3 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg">
              <div className="flex items-start gap-2">
                <AlertCircle className="w-4 h-4 text-red-600 mt-0.5" />
                <div className="text-sm">
                  <p className="font-semibold text-red-900 dark:text-red-100">주의</p>
                  <p className="text-red-700 dark:text-red-300">
                    레플리카를 0으로 설정하면 서비스가 중단됩니다.
                  </p>
                </div>
              </div>
            </div>
          )}

          
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={scaling}>
            취소
          </Button>
          <Button
            onClick={handleScale}
            disabled={scaling || targetReplicas === currentReplicas}
          >
            {scaling ? (
              <>
                <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
                스케일링 중...
              </>
            ) : (
              <>
                <Scale className="w-4 h-4 mr-2" />
                스케일링 실행
              </>
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
