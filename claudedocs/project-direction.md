# K-Le-PaaS v2 Java Backend - 프로젝트 방향 정의서

> 작성일: 2026-02-10
> 최종 수정: 2026-02-10 (아키텍처 결정 사항 확정)
> 브랜치: feat/#1-entity-setting
> 기반: 기존 Python(FastAPI) 코드베이스 분석 + 현재 Java 구현 상태

---

## 1. 프로젝트 개요

### 목표
주니어 개발자가 **자연어(AI)로 Kubernetes 애플리케이션을 배포**할 수 있는 PaaS 플랫폼.
Python(FastAPI) 모노레포에서 **Java Spring Boot**로 백엔드를 리팩토링한다.

### 핵심 원칙
- **단순화**: 기존 142개 엔드포인트 → 핵심 기능 중심으로 정리
- **아키텍처 개선**: Git Clone → ZIP Streaming(Source Staging), Webhook → 사용자 트리거
- **멀티 클라우드**: Strategy Pattern으로 NCP 우선, AWS 확장 가능
- **엔터프라이즈 구조**: Controller → Service → Repository → Entity 레이어드 아키텍처

---

## 2. 기존 프로젝트와의 핵심 차이점

| 항목 | 기존 Python | Java v2 |
|------|------------|---------|
| **프레임워크** | FastAPI + Uvicorn | Spring Boot 4.0.2 |
| **배포 트리거** | GitHub Webhook (자동) | **사용자 UI 요청 (수동)** |
| **소스 전달** | Git Clone → SourceCommit Push | **GitHub ZIP → Object Storage Streaming** |
| **K8s 매니페스트** | 소스코드 내 수정 후 push | **플랫폼 기본 Helm Chart + 설정값 주입** |
| **AI 모델** | Claude + GPT-4 + Gemini (3종) | Gemini 단일 (기존 Python AI 워커 활용) |
| **DB** | SQLite(dev) / PostgreSQL(prod) | H2(dev) / PostgreSQL(prod) |
| **테이블 수** | 11개 | 핵심 중심으로 재설계 |
| **MCP 프로토콜** | FastMCP 마운트 | 미적용 (REST API 중심) |
| **프론트엔드** | Next.js 15 + React 19 | **기존 프론트엔드 재사용** |

---

## 3. 기술 스택 (확정)

### 백엔드

| 계층 | 기술 | 비고 |
|------|------|------|
| Framework | Spring Boot 4.0.2 | Java 17 |
| ORM | Spring Data JPA | Hibernate |
| DB (dev) | H2 | 인메모리 |
| DB (prod) | PostgreSQL | |
| Cloud SDK | AWS SDK V2 (S3) | NCP Object Storage 호환 |
| HTTP Client | Spring RestClient | GitHub API, NCP API 호출 |
| Build Tool | Gradle | |
| Validation | Spring Validation (Jakarta) | |
| Lombok | Getter only, Builder | @Data 사용 금지 |
| JSON 직렬화 | Jackson (snake_case) | 프론트엔드 호환을 위해 `SNAKE_CASE` 설정 필수 |

### 프론트엔드 (기존 코드 재사용)

| 기술 | 버전 | 비고 |
|------|------|------|
| Next.js (App Router) | 15.5.6 | SSR 지원 |
| React | 19 | |
| TypeScript | 5 | |
| TailwindCSS | 4.1.9 | |
| Radix UI + shadcn/ui | - | 30+ 컴포넌트 |
| react-hook-form + Zod | 7.60.0 / 3.25.67 | 폼 관리 + 검증 |
| Recharts | latest | 차트 |

> **중요**: 기존 프론트엔드가 snake_case JSON 필드를 기대하므로, Spring Boot Jackson 설정에서
> `spring.jackson.property-naming-strategy=SNAKE_CASE` 를 반드시 적용해야 한다.

---

## 4. 현재 구현 상태 (feat/#1-entity-setting)

### 4.1 완성된 부분

**Entity Layer (핵심 도메인 모델)**
- `BaseTimeEntity` - JPA Auditing (createdAt, updatedAt)
- `User` - email, name, role, providerId(GitHub)
- `SourceRepository` - owner, repoName, gitUrl, cloudVendor, user
- `Deployment` - sourceRepository, branchName, commitHash, storageObjectKey, status + 비즈니스 메서드
- `DeploymentConfig` - sourceRepository(1:1), replicas, envVars(JSON), domainUrl, containerPort (추가 필요)
- `CommandLog` - rawCommand, interpretedIntent, isExecuted

**Enum**
- `DeploymentStatus` - PENDING, UPLOADING_SOURCE, BUILDING, DEPLOYING, SUCCESS, FAILED, CANCELED
- `CloudVendor` - NCP("ncpInfraService"), AWS("awsInfraService"), ON_PREMISE("k8sInfraService")
- `Role` - (비어있음, 구현 필요)

**Repository Layer**
- `DeploymentRepository` - @EntityGraph + Pageable
- `SourceRepositoryRepository` - @EntityGraph (User fetch)

**Infrastructure**
- `CloudInfraProvider` (interface) - uploadSourceToStorage, triggerBuildAndDeploy, scaleService
- `NcpInfraService` - 스텁 구현 (메서드 비어있음)
- `S3Config` - NCP Object Storage 연결 설정
- `RestClientConfig` - GitHub API, NCP API RestClient
- `BuildResult` (record) - externalBuildId, trackingUrl

**Config**
- `JpaConfig` - JPA Auditing 활성화

### 4.2 미구현 부분

| 레이어 | 항목 | 우선순위 |
|--------|------|---------|
| **Entity** | `Role` enum 값 정의 | 높음 |
| **Entity** | `User`에 BaseTimeEntity 상속 추가 | 높음 |
| **Entity** | `DeploymentConfig.envVars`를 `@JdbcTypeCode(SqlTypes.JSON)` + `Map<String, String>`으로 변경 | 높음 |
| **Entity** | `DeploymentConfig`에 `containerPort` 필드 추가 (기본값 8080) | 높음 |
| **Repository** | `UserRepository` | 높음 |
| **Service** | `DeploymentService` (핵심 배포 로직) | 높음 |
| **Service** | `UserService` | 중간 |
| **Service** | `NcpInfraService` 실제 구현 | 높음 |
| **Service** | `CloudInfraProviderFactory` (벤더 선택) | 높음 |
| **Controller** | 전체 REST API 엔드포인트 | 높음 |
| **DTO** | Request/Response record 클래스 | 높음 |
| **Global** | GlobalExceptionHandler | 높음 |
| **Global** | 공통 응답 형식 (ApiResponse) | 높음 |
| **Security** | Spring Security + OAuth2 | 중간 |
| **AI** | AI 모듈은 기존 Python 워커 활용 | 낮음 (Java 구현 불필요) |

### 4.3 패키지 구조 개선 제안

현재 `CloudInfraProvider`가 `infra.entity`에 위치해 있으나, 이것은 엔티티가 아닌 **서비스 인터페이스**이므로 패키지 이동을 권장한다.

```
현재:  infra/entity/CloudInfraProvider.java
제안:  infra/CloudInfraProvider.java (최상위)
       또는 infra/service/CloudInfraProvider.java (서비스 패키지)
```

---

## 5. 아키텍처 결정 사항 (확정)

### 5.1 Source Staging (배포 플로우)

```
[사용자] → "배포 요청" (UI 버튼)
    ↓
[Controller] POST /api/v1/deployments
    ↓
[DeploymentService]
    1. Deployment 엔티티 생성 (PENDING)
    2. CloudInfraProviderFactory → CloudVendor별 구현체 선택
    ↓
[CloudInfraProvider.uploadSourceToStorage()]
    3. GitHub API로 ZIP InputStream 획득
       GET https://api.github.com/repos/{owner}/{repo}/zipball/{ref}
    4. S3Client로 Object Storage에 스트리밍 업로드 (Zero-copy)
       PutObject → builds/{deploymentId}/source.zip
    5. Deployment 상태 → UPLOADING_SOURCE → BUILDING
    ↓
[CloudInfraProvider.triggerBuildAndDeploy()]
    6. NCP SourceBuild API 호출 (Object Storage 경로 전달)
    7. 빌드 상태 폴링 (Exponential Backoff: 10s → 30s → 60s)
    8. Deployment 상태 → DEPLOYING → SUCCESS/FAILED
    ↓
[Helm 배포]
    9. 플랫폼 기본 Helm Chart로 K8s 배포
    10. DeploymentConfig 값(replicas, envVars 등) --set으로 주입
```

### 5.2 Strategy Pattern (멀티 클라우드)

```
CloudInfraProvider (Interface)
    ├── NcpInfraService ("ncpInfraService")  ← 1차 구현
    ├── AwsInfraService ("awsInfraService")  ← 2차 확장
    └── K8sInfraService ("k8sInfraService")  ← On-Premise

CloudInfraProviderFactory
    → CloudVendor.getBeanName()으로 ApplicationContext에서 Bean 조회
    → SourceRepository.cloudVendor 기반 자동 선택
```

### 5.3 K8s 매니페스트 관리 — 플랫폼 기본 Helm Chart

**결정**: 플랫폼이 기본 Helm Chart를 제공하고, 사용자 설정값을 주입하는 방식.

**근거**: K-Le-PaaS의 타겟은 주니어 개발자이므로 Helm Chart 작성을 요구하면 안 된다.

```
사용자가 제공하는 것:
  - 소스코드 (GitHub Repository)
  - Dockerfile (선택, 없으면 빌드팩 사용)

플랫폼이 자동 처리하는 것:
  - 기본 Helm Chart 생성 (Deployment, Service, Ingress)
  - DeploymentConfig 값 주입:
    - replicas (minReplicas, maxReplicas)
    - containerPort (기본 8080, 사용자 설정 가능)
    - envVars (Map<String, String> → ConfigMap으로 주입)
    - domainUrl (Ingress host)
  - helm install --set image.tag={buildTag} --set replicaCount={n} --set containerPort={port} ...

고급 사용자 옵션:
  - 소스코드 내 charts/ 디렉토리가 있으면 커스텀 Chart 사용 가능
```

### 5.4 빌드 상태 확인 — 폴링 (Exponential Backoff)

**결정**: 폴링 방식, 지수 백오프 적용.

**근거**: Webhook/콜백은 공개 엔드포인트 노출, 보안/네트워크 설정 복잡도가 높아 MVP에서 과도한 엔지니어링.

```
구현 방식:
  - @Scheduled 또는 비동기 태스크로 NCP Build 상태 체크
  - 폴링 간격: 10s → 30s → 60s (Exponential Backoff)
  - 최대 대기: 30분 (타임아웃 시 FAILED 처리)
  - 성공/실패 시 즉시 폴링 중단
```

### 5.5 AI 모듈 — 기존 Python 워커 활용

**결정**: Gemini 단일 모델. Java에서 직접 구현하지 않고 기존 Python AI 워커를 활용.

**근거**: AI/NLP 로직은 이미 Python에 성숙한 구현이 있으며, Java로 재작성하는 것은 비효율적.

```
아키텍처:
  [Java Backend] → HTTP/gRPC → [Python AI Worker (Gemini)]
                                    ↓
                              자연어 해석 → 의도(Intent) + 파라미터
                                    ↓
  [Java Backend] ← 응답 ← [Python AI Worker]
        ↓
  의도에 따른 액션 실행 (배포, 스케일링 등)
```

### 5.6 인증/인가

**결정**: GitHub OAuth2 우선. Google OAuth는 후순위.

기존 Python에서의 개선 사항:
- 하드코딩 Admin 제거 → 환경변수 또는 DB 기반
- JWT HS256 유지하되 시크릿 키 강화 (`openssl rand -hex 32`)
- GitHub OAuth2 중심
- Role: `USER`, `ADMIN` (최소 2개)
- Spring Security + OAuth2 Client 활용

### 5.7 Slack 알림 — Phase 6 (고도화)

**결정**: 핵심 배포 루프(Deploy → Build → Run) 완성 후 Phase 6에서 구현.

```
준비 작업 (Phase 2):
  - NotificationService 인터페이스만 정의
  - 알림 발송 포인트에 인터페이스 호출 배치

실제 구현 (Phase 6):
  - SlackNotificationService 구현
  - Webhook/OAuth 기반 Slack 연동
```

### 5.8 프론트엔드 — 기존 Next.js 15 + React 19 재사용

**결정**: 기존 프론트엔드 코드베이스를 그대로 재사용.

**필수 호환성 요구사항**:
- API 경로: 기존 `/api/v1/...` 패턴 유지
- JSON 필드: **snake_case** (Jackson `SNAKE_CASE` 설정)
- 응답 형식: `{ "status": "success", "data": { ... } }` 패턴 유지
- CORS: 개발 환경에서 `localhost:3000` 허용

### 5.9 응답 형식 (프론트엔드 호환)

```java
// 공통 성공 응답 (snake_case로 직렬화됨)
public record ApiResponse<T>(
    String status,  // "success"
    T data,
    String message
) {}

// 공통 에러 응답
public record ErrorResponse(
    String error,
    String code,
    String message,
    String path
) {}
```

### 5.10 Entity JSON 필드 컨벤션

**결정**: JSON 필드는 `@JdbcTypeCode(SqlTypes.JSON)`을 사용하여 DB 중립적으로 처리한다.

**근거**: Hibernate 6+에서 지원하는 기능으로, H2에서는 TEXT, PostgreSQL에서는 jsonb로 자동 매핑되어
개발/운영 환경 전환 시 코드 변경이 불필요하다.

```java
// 적용 대상: DeploymentConfig.envVars
@JdbcTypeCode(SqlTypes.JSON)
private Map<String, String> envVars;  // 수동 JSON 파싱 불필요, 타입 안전
```

### 5.11 데이터 모델 전략

기존 11개 테이블에서 **1차 핵심 테이블**만 Java로 마이그레이션:

| Java Entity | 기존 Python 테이블 | 비고 |
|-------------|-------------------|------|
| `User` | (implicit, OAuth) | OAuth 기반 사용자 |
| `SourceRepository` | `user_repositories` + `user_project_integrations` | 통합/단순화 |
| `Deployment` | `deployment_histories` | 35필드 → 핵심만 |
| `DeploymentConfig` | `deployment_configs` | 거의 동일 |
| `CommandLog` | `command_history` | 거의 동일 |

**2차 확장 시 추가 검토:**
- `OAuthToken` (토큰 저장)
- `AuditLog` (감사 로그)
- `Notification` (알림)
- `DeploymentUrl` (배포 URL)

---

## 6. 구현 로드맵

### Phase 1: 기반 완성 (현재 브랜치)
- [x] Entity 설계 및 구현
- [x] Repository 구현
- [x] Infrastructure 인터페이스 설계
- [x] `Role` enum 값 정의 (USER, ADMIN)
- [x] `User`에 BaseTimeEntity 상속 추가
- [x] `DeploymentConfig.envVars` → `@JdbcTypeCode(SqlTypes.JSON)` + `Map<String, String>` 변경
- [x] `DeploymentConfig`에 `containerPort` 필드 추가 (기본값 8080)
- [x] `UserRepository` 생성
- [x] `CloudInfraProvider` 패키지 위치 정리
- [x] Jackson snake_case 설정 추가

### Phase 2: 서비스 레이어 + API 뼈대
- [ ] `GlobalExceptionHandler` 구현
- [ ] `ApiResponse` / `ErrorResponse` 공통 응답 형식
- [ ] `NotificationService` 인터페이스 정의 (Slack 준비)
- [ ] `DeploymentService` 구현
- [ ] `UserService` 구현
- [ ] `CloudInfraProviderFactory` 구현
- [ ] Request/Response DTO (record) 정의
- [ ] Controller 엔드포인트 구현
  - `DeploymentController` (배포 CRUD, 상태 조회)
  - `RepositoryController` (저장소 등록/조회)
  - `UserController` (사용자 정보)
  - `SystemController` (헬스체크)

### Phase 3: 인증/인가
- [ ] Spring Security 설정
- [ ] GitHub OAuth2 로그인 플로우
- [ ] JWT 토큰 발급/검증 (HS256, 강력한 시크릿)
- [ ] Role 기반 접근 제어 (USER, ADMIN)

### Phase 4: NCP 인프라 구현
- [ ] `NcpInfraService.uploadSourceToStorage()` 실제 구현
  - GitHub ZIP 스트리밍 → S3 Object Storage (Zero-copy)
- [ ] `NcpInfraService.triggerBuildAndDeploy()` 실제 구현
  - NCP SourceBuild API 연동
  - HMAC-SHA256 서명 생성
- [ ] 빌드 상태 폴링 (Exponential Backoff: 10s → 30s → 60s, 최대 30분)
- [ ] 플랫폼 기본 Helm Chart로 K8s 배포
- [ ] NCP SourceDeploy 연동

### Phase 5: AI 자연어 명령 (Python 워커 연동)
- [ ] Python AI Worker 통신 인터페이스 (HTTP/gRPC)
- [ ] 자연어 → 의도 해석 결과 수신
- [ ] CommandLog 기반 이력 관리
- [ ] 의도별 액션 디스패처 (배포, 스케일링, 조회 등)

### Phase 6: 고도화
- [ ] Slack 알림 연동 (SlackNotificationService)
- [ ] 모니터링 (Prometheus 메트릭)
- [ ] AWS 벤더 확장 (AwsInfraService)
- [ ] 감사 로그 (AuditLog)
- [ ] Rate Limiting
- [ ] Google OAuth 추가 (선택)

---

## 7. API 엔드포인트 계획 (1차)

기존 142개 → **핵심 ~25개로 축소**

### 인증
```
GET  /api/v1/auth/oauth2/url/{provider}    # OAuth 인증 URL 생성
POST /api/v1/auth/oauth2/login             # OAuth 로그인 처리
GET  /api/v1/auth/me                       # 현재 사용자 정보
POST /api/v1/auth/refresh                  # 토큰 갱신
```

### 저장소
```
POST /api/v1/repositories                  # 저장소 등록
GET  /api/v1/repositories                  # 내 저장소 목록
GET  /api/v1/repositories/{id}             # 저장소 상세
DELETE /api/v1/repositories/{id}           # 저장소 삭제
```

### 배포
```
POST /api/v1/deployments                   # 배포 요청
GET  /api/v1/deployments                   # 배포 목록 (페이징)
GET  /api/v1/deployments/{id}              # 배포 상세
GET  /api/v1/deployments/{id}/status       # 배포 상태
GET  /api/v1/deployments/{id}/logs         # 배포 로그
```

### 배포 설정
```
GET  /api/v1/repositories/{id}/config      # 배포 설정 조회
PUT  /api/v1/repositories/{id}/config      # 배포 설정 변경
```

### 스케일링
```
POST /api/v1/deployments/{id}/scale        # 스케일링
POST /api/v1/deployments/{id}/restart      # 재시작
```

### AI 명령
```
POST /api/v1/commands                      # 자연어 명령 실행
GET  /api/v1/commands/history              # 명령 이력
GET  /api/v1/commands/suggestions          # 명령 제안
```

### 시스템
```
GET  /api/v1/system/health                 # 헬스체크
GET  /api/v1/system/version                # 버전 정보
```

---

## 8. 결정 사항 요약

모든 미결정 사항이 확정되었다.

| # | 항목 | 결정 | 비고 |
|---|------|------|------|
| 1 | AI 서비스 | **Gemini 단일**, 기존 Python 워커 활용 | Java에서 AI 직접 구현 안 함 |
| 2 | K8s 매니페스트 | **플랫폼 기본 Helm Chart** + 설정값 주입 | 사용자에게 Helm 지식 요구 안 함 |
| 3 | 빌드 상태 확인 | **폴링 (Exponential Backoff)** | 10s → 30s → 60s, 최대 30분 |
| 4 | Slack 연동 | **Phase 6** (고도화) | Phase 2에서 인터페이스만 정의 |
| 5 | 프론트엔드 | **기존 Next.js 15 + React 19 재사용** | snake_case JSON 호환 필수 |
| 6 | Google OAuth | **후순위** (GitHub OAuth만 우선) | Phase 6에서 선택적 추가 |
| 7 | MCP 프로토콜 | **REST API만** | MCP 미적용 |

---

---

## 9. Git Workflow

### 브랜치 전략 (Git Flow)

```
main (= production)
  └── develop (= 통합 브랜치)
        └── feat/#N-기능명 (= 기능 브랜치)
```

### Issue ↔ Branch ↔ PR 매핑

| Phase | GitHub Issue | Branch | PR Target |
|-------|-------------|--------|-----------|
| Phase 1 | `#1 Entity 기반 설계 및 구현` | `feat/#1-entity-setting` | develop |
| Phase 2 | `#2 서비스 레이어 + API 뼈대` | `feat/#2-service-api` | develop |
| Phase 3 | `#3 인증/인가 (GitHub OAuth2)` | `feat/#3-auth` | develop |
| Phase 4 | `#4 NCP 인프라 구현` | `feat/#4-ncp-infra` | develop |
| Phase 5 | `#5 AI 자연어 명령 연동` | `feat/#5-ai-command` | develop |
| Phase 6 | `#6 고도화 (Slack, 모니터링 등)` | `feat/#6-advanced` | develop |

### 커밋 컨벤션

```
feat: 새 기능 추가
fix: 버그 수정
refactor: 리팩토링 (기능 변경 없음)
chore: 빌드, 설정, 의존성 변경
docs: 문서 변경
```

### 작업 흐름

```
1. GitHub 웹에서 Issue 생성
2. feat/#N-기능명 브랜치 생성
3. 구현 → 커밋 (논리적 단위별)
4. Push → PR 생성 (develop 대상)
5. 리뷰 → Merge
```

---

> 이 문서는 기존 Python 코드베이스 분석과 현재 Java 구현 상태를 기반으로 작성되었습니다.
> 모든 아키텍처 결정 사항이 확정되었으며, Phase 1부터 순차적으로 구현을 진행합니다.
