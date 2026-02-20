# feat/#15-ncp-infra: NCP 인프라 Kaniko 빌드 파이프라인 구현

## Context
GitHub 저장소 소스를 빌드해 컨테이너 이미지를 만들고 NCP Container Registry(NCR)에 푸시한 뒤 NKS 클러스터에 배포하는 CI/CD 파이프라인 실제 구현.
초기 설계는 NCP SourceBuild를 사용하는 방안이었으나, NCP SourceBuild가 ObjectStorage를 source.type으로 지원하지 않아 Kaniko K8s Job 방식으로 전환.

## 변경 사항

### 1. NCP SourceBuild → Kaniko K8s Job 전환
- **삭제**: `NcpSourceBuildClient`, `NcpApiSigner`, 관련 DTO 5개
- **사유**: NCP SourceBuild의 유효한 source.type이 SourceCommit/GitHub/Bitbucket 등 Git 직접 연결만 가능. Object Storage ZIP 경유 불가.
- **신규**: `NcpInfraService`가 fabric8 Kubernetes client로 Kaniko Job 직접 생성

### 2. Kaniko S3 context → initContainer + 로컬 컨텍스트 전환
초기 구현: `--context=s3://bucket/key` (Kaniko가 Object Storage 직접 읽기)
- 문제 1: Kaniko S3 context는 `.tar.gz` 포맷만 지원, ZIP 업로드 시 `gzip: invalid header`
- 문제 2: `S3_ENDPOINT` env var가 Kaniko 내부 AWS SDK에서 보장 안 됨 (NCP 커스텀 endpoint 문제)

최종 구현: initContainer 패턴
```
initContainer(source-downloader: amazon/aws-cli)
  → aws s3 cp --endpoint-url {NCP_ENDPOINT} → /tmp/source.zip
  → python3 unzip → /workspace (emptyDir)

kaniko container
  → --context=dir:///workspace (S3 endpoint 의존성 완전 제거)
  → --destination={NCR_ENDPOINT}/{image}:latest
```

### 3. @Async + @Transactional 타이밍 버그 수정
- **문제**: `createDeployment()` 내에서 `@Async` 파이프라인 직접 호출 → 트랜잭션 커밋 전에 새 스레드가 DB 조회 → "배포를 찾을 수 없습니다"
- **수정**: `TransactionSynchronizationManager.registerSynchronization(afterCommit → executePipeline)`

### 4. DeploymentRepository.findById() @EntityGraph 추가
- 비동기 파이프라인의 각 단계(`REQUIRES_NEW`)마다 `sourceRepository` 필요
- `findById()`를 `@EntityGraph(attributePaths = {"sourceRepository"})`로 오버라이드

### 5. Kaniko Pod 실패 원인 감지 강화
- `Job.status.failed > 0`일 때도 `detectPodFailure()` 호출
- initContainer(source-downloader) `terminated.exitCode` 체크 추가

## 파이프라인 흐름 (최종)
```
POST /api/v1/deployments
  → DeploymentService.createDeployment() [@Transactional]
    → Deployment INSERT
    → afterCommit 콜백 등록
  → 트랜잭션 커밋
  → [새 스레드] DeploymentPipelineService.executePipeline()
      1. executeUpload()   [REQUIRES_NEW]
         - GitHub ZIP 다운로드 (Installation Token)
         - NCP Object Storage 업로드 (builds/{id}/source.zip)
         - 상태: UPLOADING_SOURCE → UPLOADED
      2. executeBuildTrigger()  [REQUIRES_NEW]
         - Kaniko K8s Job 생성 (klepaas-build-{id})
         - 상태: BUILDING
      3. pollBuildStatus() [Exponential Backoff: 10s→20s→60s, max 30분]
         - Job.status.succeeded/failed 폴링
         - initContainer/Pod 조기 실패 감지
      4. executeK8sDeploy()  [REQUIRES_NEW]
         - fabric8로 Deployment + Service 생성
         - 상태: DEPLOYING
      5. markSuccess()  [REQUIRES_NEW]
         - 상태: SUCCESS
```

## NCP 인프라 구성
| 리소스 | 내용 |
|--------|------|
| Object Storage 버킷 | `k-le-paas-build-source` |
| Container Registry | `klepaas.kr.ncr.ntruss.com` |
| NKS 클러스터 | `klepaas` (노드 1개) |
| K8s Secret | `ncp-cr` (docker-registry, NCR 인증) |
| Kaniko Job TTL | 3600초 (완료 후 자동 삭제) |

## 환경변수
```
NCP_ACCESS_KEY, NCP_SECRET_KEY
NCP_STORAGE_BUCKET=k-le-paas-build-source
NCR_ENDPOINT=klepaas.kr.ncr.ntruss.com
K8S_NAMESPACE=default
K8S_IMAGE_PULL_SECRET=ncp-cr
KANIKO_IMAGE=gcr.io/kaniko-project/executor:latest
```

---

# Phase 5: AI 자연어 명령 (Java 재구현)

## Context
기존 Python(FastAPI) 백엔드에 내장된 AI/NLP 기능을 Java Spring Boot로 재구현한다.
Python 워커는 독립 서비스가 아니라 FastAPI에 내장되어 있었으므로, Java에서도 동일하게 백엔드에 내장한다.
Gemini REST API를 직접 호출하고, 파싱된 Intent를 기존 Java 서비스(DeploymentService, RepositoryService)에 디스패치한다.

## Git 워크플로우
1. main 체크아웃 + pull
2. GitHub 이슈 생성
3. `feat/#N-ai-nlp` 브랜치 생성
4. 구현
5. 커밋 + 푸시 + PR 생성

## 아키텍처

```
사용자 → POST /api/v1/nlp/command → NlpController
    → NlpCommandService (오케스트레이터)
        → GeminiClient (Gemini REST API 호출)
        → IntentParser (JSON 응답 → ParsedIntent)
        → ActionDispatcher (Intent → 기존 서비스 메서드 호출)
        → CommandLog DB 저장
    → 응답 반환
```

## 신규 파일 (20개)

### Entity & Enum
| 파일 | 설명 |
|------|------|
| `ai/entity/Intent.java` | DEPLOY, SCALE, RESTART, STATUS, LOGS, LIST_DEPLOYMENTS, LIST_REPOSITORIES, OVERVIEW, HELP, UNKNOWN |
| `ai/entity/RiskLevel.java` | LOW, MEDIUM, HIGH |
| `ai/entity/ConversationSession.java` | 대화 세션 엔티티 (sessionToken UUID, active, lastActiveAt) |

### Client
| 파일 | 설명 |
|------|------|
| `ai/config/GeminiConfig.java` | geminiRestClient Bean 설정 |
| `ai/client/GeminiClient.java` | Gemini REST API 호출 (RestClient 패턴) |
| `ai/client/dto/GeminiRequest.java` | Gemini API 요청 record |
| `ai/client/dto/GeminiResponse.java` | Gemini API 응답 record |

### Service
| 파일 | 설명 |
|------|------|
| `ai/service/NlpCommandService.java` | 핵심 오케스트레이터 (프롬프트 구성 → Gemini 호출 → 파싱 → 리스크 판단 → 실행/대기) |
| `ai/service/IntentParser.java` | Gemini JSON 응답을 ParsedIntent로 변환 (마크다운 코드블록 처리 포함) |
| `ai/service/ActionDispatcher.java` | Intent → 기존 서비스 메서드 매핑 + 리스크 분류 |

### DTO
| 파일 | 설명 |
|------|------|
| `ai/dto/NlpCommandRequest.java` | `{command, sessionId?}` |
| `ai/dto/NlpCommandResponse.java` | `{commandLogId, intent, message, result, riskLevel, requiresConfirmation, sessionId}` |
| `ai/dto/NlpConfirmRequest.java` | `{commandLogId, confirmed}` |
| `ai/dto/ParsedIntent.java` | 내부용: `{intent, args, confidence, message}` |
| `ai/dto/CommandLogResponse.java` | 이력 조회 응답 |

### Controller & Repository & Exception
| 파일 | 설명 |
|------|------|
| `ai/controller/NlpController.java` | `POST /command`, `POST /confirm`, `GET /history` |
| `ai/repository/CommandLogRepository.java` | JPA Repository |
| `ai/repository/ConversationSessionRepository.java` | JPA Repository |
| `ai/exception/AiProcessingException.java` | BusinessException 서브클래스 |

### Resource
| 파일 | 설명 |
|------|------|
| `src/main/resources/prompts/system-prompt.txt` | 시스템 프롬프트 (Intent 정의, JSON 응답 형식, 한국어 처리 규칙) |

## 수정 파일 (3개)

| 파일 | 변경 내용 |
|------|----------|
| `ai/entity/CommandLog.java` | 필드 추가: intentArgs, riskLevel, requiresConfirmation, confirmed, executionResult, aiResponse, session / interpretedIntent를 String→Intent enum 변경 |
| `global/exception/ErrorCode.java` | AI_API_ERROR, AI_PARSE_ERROR, COMMAND_LOG_NOT_FOUND, SESSION_NOT_FOUND 추가 |
| `application.yaml` | `gemini.api.base-url`, `gemini.api.key`, `gemini.api.model` 설정 추가 |

## 테스트 파일 (5개)

| 파일 | 유형 |
|------|------|
| `ai/service/IntentParserTest.java` | Unit (JSON 파싱, 마크다운 처리, 잘못된 응답 처리) |
| `ai/service/ActionDispatcherTest.java` | Unit (리스크 분류, 서비스 메서드 호출 검증) |
| `ai/service/NlpCommandServiceTest.java` | Unit (LOW 자동실행, MEDIUM/HIGH 확인 대기, 확인/취소 흐름) |
| `ai/client/GeminiClientTest.java` | Unit (RestClient 모킹) |
| `ai/controller/NlpControllerTest.java` | WebMvcTest (인증, 요청/응답 구조 검증) |

## API 엔드포인트

| Method | URL | 설명 | 인증 |
|--------|-----|------|------|
| POST | `/api/v1/nlp/command` | 자연어 명령 처리 | 필요 |
| POST | `/api/v1/nlp/confirm` | 위험 명령 확인/취소 | 필요 |
| GET | `/api/v1/nlp/history` | 명령 이력 조회 (페이징) | 필요 |

## 리스크 분류

| Risk | Intent | 동작 |
|------|--------|------|
| LOW | STATUS, LOGS, LIST_DEPLOYMENTS, LIST_REPOSITORIES, OVERVIEW, HELP, UNKNOWN | 즉시 실행 |
| MEDIUM | SCALE, RESTART | 확인 후 실행 |
| HIGH | DEPLOY | 확인 후 실행 |

## 구현 순서

### Step 1: 설정 및 기반
- application.yaml에 gemini 설정 추가
- ErrorCode에 AI 에러 코드 추가
- GeminiConfig, GeminiRequest, GeminiResponse, AiProcessingException 생성

### Step 2: Entity, Enum, Repository
- Intent, RiskLevel enum 생성
- CommandLog 엔티티 수정 (필드 추가)
- ConversationSession 엔티티 생성
- CommandLogRepository, ConversationSessionRepository 생성

### Step 3: Gemini Client + IntentParser
- GeminiClient 생성
- IntentParser 생성
- IntentParserTest 작성 및 실행

### Step 4: ActionDispatcher
- ActionDispatcher 생성
- ActionDispatcherTest 작성 및 실행

### Step 5: DTO
- NlpCommandRequest, NlpCommandResponse, NlpConfirmRequest, ParsedIntent, CommandLogResponse 생성

### Step 6: NlpCommandService + 시스템 프롬프트
- system-prompt.txt 생성
- NlpCommandService 생성
- NlpCommandServiceTest 작성 및 실행

### Step 7: Controller
- NlpController 생성
- NlpControllerTest 작성 및 실행

### Step 8: 전체 검증
- `./gradlew test` 전체 테스트 통과 확인
- 애플리케이션 기동 확인

## 개선 사항 (기존 Python 대비)
- 시스템 프롬프트를 별도 리소스 파일로 분리 (Python: 코드 내 337행 하드코딩)
- Redis 제거 → DB 기반 세션 관리로 단순화
- Gemini 모델명 설정으로 외부화 (Python: 하드코딩)
- Intent를 enum으로 타입 안전하게 관리
- 기존 Java 서비스와 직접 연동 (Python: K8s 직접 조작 → Java: 서비스 레이어 통해 실행)
