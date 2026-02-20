"use client"

import { useEffect, useMemo, useState } from "react"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { useToast } from "@/hooks/use-toast"
import { SettingsHeader } from "./SettingsHeader"
import { OrganizationSection, OrganizationSettings } from "./OrganizationSection"
import { EnvironmentsClustersSection, EnvironmentItem } from "./EnvironmentsClustersSection"
import { SlackNotificationsSection, SlackSettings } from "./SlackNotificationsSection"
import { MCPConnectorsSection, MCPSettings } from "./MCPConnectorsSection"
import { AgentsBridgesSection, AgentRow, BridgeRow } from "./AgentsBridgesSection"

type SettingsDraft = {
  organization: OrganizationSettings
  environments: EnvironmentItem[]
  slack: SlackSettings
  mcp: MCPSettings
}

const STORAGE_KEY = "kpaas_settings_draft"

const DEFAULT_DRAFT: SettingsDraft = {
  organization: { displayName: "K-Le-PaaS", locale: "ko", timezone: "Asia/Seoul" },
  environments: [
    { name: "staging", cluster: "GCP-Staging", namespace: "default", cpuQuota: "1", memQuota: "1Gi" },
    { name: "production", cluster: "NCP-Prod", namespace: "prod", cpuQuota: "2", memQuota: "2Gi" },
  ],
  slack: {
    enabled: false,
    webhookUrl: "",
    events: { deployStart: true, deploySuccess: true, deployFail: true, rollback: true },
  },
  mcp: { github: { connected: false }, claude: { enabled: false }, openai: { enabled: false } },
}

const MOCK_AGENTS: AgentRow[] = [
  { name: "ncp-agent", type: "NCP", status: "healthy", lastHeartbeat: "방금 전" },
  { name: "gcp-agent", type: "GCP", status: "healthy", lastHeartbeat: "1분 전" },
]

const MOCK_BRIDGES: BridgeRow[] = [
  { name: "rabbitmq-bridge", kind: "RabbitMQ", status: "connected" },
  { name: "prometheus-bridge", kind: "Prometheus", status: "connected" },
]

export function SettingsPage() {
  const { toast } = useToast()
  const [draft, setDraft] = useState<SettingsDraft>(DEFAULT_DRAFT)
  const [initial, setInitial] = useState<SettingsDraft | null>(null)

  // Load from localStorage
  useEffect(() => {
    try {
      const raw = typeof window !== "undefined" ? window.localStorage.getItem(STORAGE_KEY) : null
      if (raw) {
        const parsed = JSON.parse(raw) as SettingsDraft
        setDraft(parsed)
        setInitial(parsed)
      } else {
        setInitial(DEFAULT_DRAFT)
      }
    } catch {
      setInitial(DEFAULT_DRAFT)
    }
  }, [])

  const dirty = useMemo(() => JSON.stringify(draft) !== JSON.stringify(initial), [draft, initial])

  // Unsaved changes guard
  useEffect(() => {
    const handler = (e: BeforeUnloadEvent) => {
      if (!dirty) return
      e.preventDefault()
      e.returnValue = "이 페이지를 떠나면 변경 사항이 사라집니다."
    }
    window.addEventListener("beforeunload", handler)
    return () => window.removeEventListener("beforeunload", handler)
  }, [dirty])

  const handleSave = () => {
    try {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(draft))
      setInitial(draft)
      toast({ title: "설정이 저장되었습니다", description: "데모 저장소(localStorage)에 저장했습니다." })
    } catch {
      toast({ title: "저장 실패", description: "브라우저 저장소에 접근할 수 없습니다.", variant: "destructive" })
    }
  }

  const handleCancel = () => {
    if (initial) setDraft(initial)
  }

  return (
    <div className="space-y-6">
      <SettingsHeader
        title="Settings"
        subtitle="이 페이지는 프론트엔드 데모입니다. 실제 설정 적용/저장은 이루어지지 않으며, 변경 사항은 브라우저에만 임시 저장됩니다."
        onSave={handleSave}
        onCancel={handleCancel}
        saveDisabled={!dirty}
      />

      <Tabs defaultValue="organization" className="w-full">
        <TabsList>
          <TabsTrigger value="organization">조직</TabsTrigger>
          <TabsTrigger value="env">환경 & 클러스터</TabsTrigger>
          <TabsTrigger value="slack">Slack 알림</TabsTrigger>
          <TabsTrigger value="mcp">MCP 커넥터</TabsTrigger>
          <TabsTrigger value="agents">에이전트 & 브리지</TabsTrigger>
        </TabsList>

        <TabsContent value="organization" className="mt-4">
          <OrganizationSection value={draft.organization} onChange={(v) => setDraft({ ...draft, organization: v })} />
        </TabsContent>

        <TabsContent value="env" className="mt-4">
          <EnvironmentsClustersSection
            value={draft.environments}
            onChange={(v) => setDraft({ ...draft, environments: v })}
          />
        </TabsContent>

        <TabsContent value="slack" className="mt-4">
          <SlackNotificationsSection value={draft.slack} onChange={(v) => setDraft({ ...draft, slack: v })} />
        </TabsContent>

        <TabsContent value="mcp" className="mt-4">
          <MCPConnectorsSection value={draft.mcp} onChange={(v) => setDraft({ ...draft, mcp: v })} />
        </TabsContent>

        <TabsContent value="agents" className="mt-4">
          <AgentsBridgesSection agents={MOCK_AGENTS} bridges={MOCK_BRIDGES} />
        </TabsContent>
      </Tabs>
    </div>
  )
}


