"use client"

import React from "react"
import { NLPResponse, CommandErrorResponse, RollbackExecutionResponse } from "@/lib/types/nlp-response"

// 개별 렌더러들 import
import { PodListRenderer } from "./PodListRenderer"
import { RollbackListRenderer } from "./RollbackListRenderer"
import { StatusRenderer } from "./StatusRenderer"
import { PodStatusRenderer } from "./PodStatusRenderer"
import { ServiceStatusRenderer } from "./ServiceStatusRenderer"
import { DeploymentStatusRenderer } from "./DeploymentStatusRenderer"
import { ServiceListRenderer } from "./ServiceListRenderer"
import { DeploymentListRenderer } from "./DeploymentListRenderer"
import { IngressListRenderer } from "./IngressListRenderer"
import { NamespaceListRenderer } from "./NamespaceListRenderer"
import { EndpointListRenderer } from "./EndpointListRenderer"
import { LogsRenderer } from "./LogsRenderer"
import { OverviewRenderer } from "./OverviewRenderer"
import { CostAnalysisRenderer } from "./CostAnalysisRenderer"
import { DeployResponseRenderer } from "./DeployResponseRenderer"
import { ScaleRenderer } from "./ScaleRenderer"
import { RestartRenderer } from "./RestartRenderer"
import { RollbackRenderer } from "./RollbackRenderer"
import { DeploymentDetailRenderer } from "./DeploymentDetailRenderer"
import { ServiceDetailRenderer } from "./ServiceDetailRenderer"
// import { DeploymentDetailRenderer } from "./DeploymentDetailRenderer"
// import { OverviewRenderer } from "./OverviewRenderer"
// import { EndpointRenderer } from "./EndpointRenderer"
// import { ScaleRenderer } from "./ScaleRenderer"
// import { DeployRenderer } from "./DeployRenderer"
// import { RestartRenderer } from "./RestartRenderer"
// import { CostAnalysisRenderer } from "./CostAnalysisRenderer"
// import { ErrorRenderer } from "./ErrorRenderer"
import { EndpointRenderer } from "./EndpointRenderer"
import { ListCommandsRenderer } from "./ListCommandsRenderer"

interface NLPResponseRendererProps {
  response: NLPResponse
  onRollbackClick?: (version: any) => void
  onNavigateToPipelines?: () => void
}

export function NLPResponseRenderer({ response, onRollbackClick, onNavigateToPipelines }: NLPResponseRendererProps) {
  
  // 불필요한 디버그 로그 제거; 에러 상황만 렌더러에서 표기
  
  // 에러 응답 처리
  if (response.type === 'error') {
    return (
      <div className="p-4 border border-red-200 bg-red-50 dark:bg-red-950/20 rounded-lg">
        <div className="flex items-center gap-2 text-red-800 dark:text-red-200">
          <span className="font-medium">오류 발생</span>
        </div>
        <p className="text-sm text-red-700 dark:text-red-300 mt-1">
          {response.summary}
        </p>
        {response.data.formatted.error && (
          <details className="mt-2">
            <summary className="text-xs text-red-600 dark:text-red-400 cursor-pointer">
              상세 오류 정보
            </summary>
            <pre className="text-xs bg-red-100 dark:bg-red-900/30 p-2 rounded mt-1 overflow-x-auto">
              {response.data.formatted.error}
            </pre>
          </details>
        )}
      </div>
    )
  }

  // 명령어 에러 응답 처리
  // if (response.type === 'command_error') {
  //   return <CommandErrorRenderer response={response as CommandErrorResponse} />
  // }

  // 롤백 실행 응답 처리
  // if (response.type === 'rollback_execution') {
  //   return <RollbackExecutionRenderer response={response as RollbackExecutionResponse} />
  // }

  // 알 수 없는 응답 처리
  if (response.type === 'unknown') {
    const suggestions = response.data?.formatted?.suggestions || []
    return (
      <div className="p-4 border border-blue-200 bg-blue-50 dark:bg-blue-950/20 rounded-lg">
        <div className="flex items-center gap-2 text-blue-800 dark:text-blue-200">
          <span className="font-medium">❌ 명령어를 이해할 수 없습니다</span>
        </div>
        <p className="text-sm text-blue-700 dark:text-blue-300 mt-1">
          {response.summary}
        </p>
        
        {suggestions.length > 0 && (
          <div className="mt-3">
            <p className="text-sm font-medium text-blue-800 dark:text-blue-200 mb-2">
              💡 올바른 사용법:
            </p>
            <div className="space-y-1">
              {suggestions.map((suggestion: string, index: number) => (
                <div 
                  key={index}
                  className="text-xs bg-blue-100 dark:bg-blue-900/30 text-blue-800 dark:text-blue-200 px-2 py-1 rounded border border-blue-200 dark:border-blue-800"
                >
                  {suggestion}
                </div>
              ))}
            </div>
          </div>
        )}
        
        <details className="mt-3">
          <summary className="text-xs text-blue-600 dark:text-blue-400 cursor-pointer">
            ▼ 원본 데이터
          </summary>
          <pre className="text-xs bg-blue-100 dark:bg-blue-900/30 p-2 rounded mt-1 overflow-x-auto">
            {JSON.stringify(response.data.formatted, null, 2)}
          </pre>
        </details>
      </div>
    )
  }

  // 타입별 렌더러 분기
  switch (response.type) {
    case 'list_pods':
      return <PodListRenderer response={response} />
    
    case 'list_rollback':
      return <RollbackListRenderer response={response} onRollbackClick={onRollbackClick} />
    
    case 'status':
      return <StatusRenderer response={response} />
    
    case 'pod_status':
      return <PodStatusRenderer response={response} />
    
    case 'service_status':
      return <ServiceStatusRenderer response={response} />
    
    case 'deployment_status':
      return <DeploymentStatusRenderer response={response} />
    
    case 'list_services':
      return <ServiceListRenderer response={response} />
    
    case 'list_deployments':
      return <DeploymentListRenderer response={response} />
    
    case 'list_ingresses':
      return <IngressListRenderer response={response} />
    
    case 'list_namespaces':
      return <NamespaceListRenderer response={response} />
    
    case 'logs':
      return <LogsRenderer response={response} />

    case 'list_endpoints':
      return <EndpointListRenderer response={response} />
    
    case 'overview':
      return <OverviewRenderer response={response} />
    
    case 'cost_analysis':
      return <CostAnalysisRenderer response={response} />
    
    case 'deploy':
    case 'deploy_github_repository':
      return <DeployResponseRenderer response={response} onNavigateToPipelines={onNavigateToPipelines} />
    
    case 'get_service':
      return <ServiceDetailRenderer response={response} />
    
    case 'get_deployment':
      return <DeploymentDetailRenderer response={response} />
    
    // case 'overview':
    //   return <OverviewRenderer response={response} />
    
    case 'endpoint':
      return <EndpointRenderer response={response} />
    
    case 'scale':
      return <ScaleRenderer response={response} />
    
    case 'restart':
      return <RestartRenderer response={response} />
    
    case 'rollback':
      return <RollbackRenderer response={response} />

    case 'list_commands':
      return <ListCommandsRenderer response={response} />

    // case 'deploy':
    //   return <DeployRenderer response={response} />

    // case 'cost_analysis':
    //   return <CostAnalysisRenderer response={response} />

    default:
      // 기본 JSON 렌더링 (임시)
      return (
        <div className="p-4 border border-gray-200 bg-gray-50 dark:bg-gray-950/20 rounded-lg">
          <div className="flex items-center gap-2 text-gray-800 dark:text-gray-200 mb-2">
            <span className="font-medium">{response.type}</span>
          </div>
          <p className="text-sm text-gray-700 dark:text-gray-300 mb-3">
            {response.summary}
          </p>
          <details>
            <summary className="text-xs text-gray-600 dark:text-gray-400 cursor-pointer">
              상세 데이터
            </summary>
            <pre className="text-xs bg-gray-100 dark:bg-gray-900/30 p-2 rounded mt-1 overflow-x-auto">
              {JSON.stringify(response.data.formatted, null, 2)}
            </pre>
          </details>
        </div>
      )
  }
}
