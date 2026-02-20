# K-Le-PaaS 코드베이스 종합 분석 보고서

> 분석일: 2026-02-10
> 분석 범위: K-Le-PaaS 전체 모노레포 (backend-hybrid, frontend, agents, protocol-bridges, integration-tests)

---

## 목차

1. [프로젝트 구조 & 기술 스택](#1-프로젝트-구조--기술-스택)
2. [API 엔드포인트](#2-api-엔드포인트-총-142개)
3. [인증/인가](#3-인증인가)
4. [빌드/배포 파이프라인 (NCP 연동)](#4-빌드배포-파이프라인-ncp-연동)
5. [데이터 모델](#5-데이터-모델-11개-테이블)
6. [프론트엔드 연동](#6-프론트엔드-연동)
7. [설정/환경변수](#7-환경변수-전체-목록)
8. [에러 처리](#8-에러-처리)
9. [보안 권고사항](#9-보안-권고사항)

---

## 1. 프로젝트 구조 & 기술 스택

### 디렉토리 구조

```
C:\klepaas\
├── backend-hybrid/          # FastAPI + MCP 하이브리드 백엔드
│   ├── app/
│   │   ├── api/v1/         # REST API 라우터 (22개 파일)
│   │   ├── mcp/            # MCP 도구 + 외부 커넥터
│   │   │   ├── tools/      # 내부 MCP 도구 (deploy, k8s, rollback, monitor 등)
│   │   │   └── external/   # 외부 MCP 커넥터 (GitHub, Slack)
│   │   ├── llm/            # 다중 AI 모델 (Claude, GPT-4, Gemini)
│   │   ├── services/       # 비즈니스 로직 (50+ 파일)
│   │   ├── models/         # SQLAlchemy ORM 모델
│   │   └── core/           # 설정, 보안, 에러 핸들러
│   ├── alembic/            # DB 마이그레이션
│   └── tests/              # 백엔드 테스트
├── frontend/               # Next.js 15 + React 19 대시보드
│   ├── app/                # Next.js App Router (페이지)
│   ├── components/         # React 컴포넌트
│   ├── contexts/           # React Context (인증 등)
│   ├── hooks/              # 커스텀 React 훅
│   └── lib/                # API 클라이언트, 유틸리티, 타입
├── protocol-bridges/       # PostgreSQL, RabbitMQ, Prometheus MCP 브릿지
│   ├── postgresql-bridge/
│   ├── rabbitmq-bridge/
│   └── prometheus-bridge/
├── agents/                 # MCP-native Git 에이전트
│   ├── gcp-agent/          # GCP GKE 배포
│   └── ncp-agent/          # NCP NKS 배포
├── integration-tests/      # Docker Compose 기반 E2E 테스트
│   ├── tests/              # 테스트 케이스 (E2E, Locust 성능)
│   ├── scripts/            # 벤치마크, 분석 스크립트
│   └── docker-compose.yml  # 통합 테스트 환경
├── deployment-config/      # Helm 차트 & K8s 매니페스트
│   ├── apps/
│   ├── charts/
│   └── root-app.yaml
├── infrastructure/         # Terraform IaC
└── .github/workflows/      # CI/CD 파이프라인
```

### 기술 스택

| 계층 | 기술 | 버전 |
|------|------|------|
| **백엔드 프레임워크** | FastAPI + Uvicorn | 0.116.1 / 0.35.0 |
| **프론트엔드 프레임워크** | Next.js (App Router) + React | 15.5.6 / 19 |
| **언어** | Python (백엔드), TypeScript (프론트엔드) | - |
| **AI/NLP** | Anthropic Claude, OpenAI GPT-4, Google Gemini 2.0 | anthropic 0.40.0, openai 1.51.0, google-genai 0.8.0 |
| **NLP 추가** | spaCy, scikit-learn, NLTK | 3.7.2 / 1.7.2 / 3.9.2 |
| **인증** | PyJWT (HS256), OAuth2, pyotp | 2.8.0 / - / 2.9.0 |
| **ORM** | SQLAlchemy | 2.0.36 |
| **DB** | SQLite (dev) / PostgreSQL (prod) | - / psycopg 3.2.3 |
| **마이그레이션** | Alembic | - |
| **클라우드** | GCP GKE, NCP NKS | kubernetes 30.1.0, ncloud-sdk 1.1.6 |
| **MCP 프로토콜** | FastMCP | 0.1.0 |
| **모니터링** | Prometheus, Alertmanager, Grafana | prometheus-client 0.22.1 |
| **메시지 큐** | RabbitMQ (브릿지 통해) | - |
| **UI 컴포넌트** | Radix UI (30+ 컴포넌트) + shadcn/ui | - |
| **스타일링** | TailwindCSS | 4.1.9 |
| **폼/검증** | react-hook-form + Zod | 7.60.0 / 3.25.67 |
| **차트** | Recharts | latest |
| **설정 관리** | Pydantic Settings + python-dotenv | 2.6.1 / 1.0.1 |
| **캐싱** | Redis | redis[hiredis] 5.0.1 |

### 엔트리 포인트

| 컴포넌트 | 엔트리 포인트 | 포트 | 실행 명령 |
|---------|-------------|------|---------|
| **백엔드** | `backend-hybrid/app/main.py::create_app()` | 8080 | `uvicorn app.main:app --reload --port 8080` |
| **MCP 서버** | `/mcp/stream` (FastAPI 내부 마운트) | 8080 | FastAPI와 동일 프로세스 |
| **프론트엔드** | `frontend/app/layout.tsx` | 3000 | `npm run dev` |
| **통합 테스트** | Docker Compose | 다수 | `docker-compose -f integration-test.yml up` |

---

## 2. API 엔드포인트 (총 142개)

### 라우터 요약

| 라우터 | 파일 경로 (`app/api/v1/`) | 수 | 주요 기능 |
|--------|--------------------------|---|---------|
| `system` | `system.py` | 4 | 헬스체크 (`/health`, `/healthz`), 버전 정보 |
| `dashboard` | `dashboard.py` | 3 | 대시보드 개요, 메트릭, 헬스 |
| `deployments` | `deployments.py` | 13 | 배포 CRUD, 스케일링, 재시작, Pod/로그 조회 |
| `deployment_histories` | `deployment_histories.py` | 6 | 배포 이력 목록, 통계, 리포별 이력 |
| `rollback` | `rollback.py` | 6 | 커밋 롤백, 이전 버전 롤백, 진단 |
| `monitoring` | `monitoring.py` | 20 | Prometheus 쿼리, CPU/메모리/디스크/네트워크, NKS 메트릭, 알림 |
| `nlp` | `nlp.py` | 9 | 자연어 명령 처리, 대화형, 히스토리, 제안 |
| `github_workflows` | `github_workflows.py` | 14 | GitHub App 설치, 리포 연결, PR, 수동 배포 |
| `ncp_pipeline` | `ncp_pipeline_api.py` | 6 | NCP 파이프라인 생성/실행/삭제, 히스토리 |
| `cicd` | `cicd.py` | 4 | CI/CD 웹훅, GitHub App 설치 토큰 |
| `oauth2` | `oauth2.py` | 4 | OAuth2 URL 생성, 로그인, 사용자 정보, Admin 로그인 |
| `github_oauth` | `github_oauth.py` | 2 | GitHub OAuth 로그인/콜백 |
| `auth_verify` | `auth_verify.py` | 2 | JWT 토큰 검증, 현재 사용자 |
| `slack_auth` | `slack_auth.py` | 7 | Slack OAuth, 채널 조회, 설정 저장, 테스트 |
| `projects` | `projects.py` | 8 | 온보딩, SourceCommit 생성, 빌드/배포 실행 |
| `k8s` | `k8s.py` | 9 | K8s 리소스 CRUD, Namespace/Context, 헬스 |
| `admin_db` | `admin_db.py` | 10 | 테이블 목록, DB 조회, 통계, SQL 쿼리 |
| `user_url` | `user_url.py` | 4 | 배포 URL CRUD |
| `tutorial` | `tutorial.py` | 7 | 튜토리얼 시작/진행/완료/리셋 |
| `metrics` | `metrics.py` | 1 | Prometheus 메트릭 엔드포인트 |
| `websocket` | `websocket.py` | 1 | WebSocket 통계 |
| `mcp_external` | `mcp/external/api.py` | - | 외부 MCP 커넥터 API |

### 엔드포인트 통계

```
총 API 엔드포인트:          142개
  ├─ GET 요청:              64개 (45%)
  ├─ POST 요청:             60개 (42%)
  ├─ PUT 요청:              8개  (6%)
  ├─ DELETE 요청:           9개  (6%)
  └─ PATCH/OPTIONS:         1개  (1%)

MCP 도구:                   18개
```

### 주요 엔드포인트 상세

#### 인증 & OAuth

```
GET  /api/v1/auth/verify                        # JWT 토큰 검증
GET  /api/v1/auth/me                            # 현재 사용자 정보
GET  /api/v1/auth/oauth2/url/{provider}         # OAuth2 인증 URL 생성 (google/github)
POST /api/v1/auth/oauth2/login                  # OAuth2 로그인 처리
GET  /api/v1/auth/oauth2/user                   # 현재 사용자 정보
POST /api/v1/auth/oauth2/admin/login            # Admin 로그인
```

#### 배포

```
POST /api/v1/deploy                             # 애플리케이션 배포
GET  /api/v1/deployments                        # 모든 배포 조회
GET  /api/v1/deployments/{app_name}/status      # 배포 상태
POST /api/v1/deployments/{owner}/{repo}/scale   # 스케일링
POST /api/v1/deployments/{owner}/{repo}/restart # 재시작
GET  /api/v1/deployments/{owner}/{repo}/config  # 배포 설정 조회
PUT  /api/v1/deployments/{owner}/{repo}/config  # 배포 설정 변경
```

#### NLP (자연어 처리)

```
POST /api/v1/nlp/process                        # 자연어 명령 처리
POST /api/v1/nlp/conversation                   # 대화형 명령 처리
POST /api/v1/nlp/confirm                        # 사용자 확인 응답
GET  /api/v1/nlp/history                        # 명령 히스토리
GET  /api/v1/nlp/suggestions                    # 명령 제안
```

#### NCP 파이프라인

```
POST /api/v1/ncp/pipeline/create                # 파이프라인 생성
POST /api/v1/ncp/pipeline/execute               # 파이프라인 실행
GET  /api/v1/ncp/pipeline/history/{pid}/{hid}   # 히스토리 조회
GET  /api/v1/ncp/pipeline/status/{owner}/{repo} # 상태 조회
DELETE /api/v1/ncp/pipeline/{pipeline_id}       # 파이프라인 삭제
```

### MCP 도구 (18개)

`app/main.py` 라인 247-317에서 `/mcp/stream`에 마운트:

| 도구명 | 파일 | 기능 |
|--------|------|------|
| `deploy_application_tool_mcp` | `mcp/tools/deploy_app.py` | 앱 배포 |
| `k8s_create_mcp` | `mcp/tools/k8s_resources.py` | K8s 리소스 생성 |
| `k8s_get_mcp` | `mcp/tools/k8s_resources.py` | K8s 리소스 조회 |
| `k8s_apply_mcp` | `mcp/tools/k8s_resources.py` | K8s 리소스 적용 |
| `k8s_delete_mcp` | `mcp/tools/k8s_resources.py` | K8s 리소스 삭제 |
| `rollback_deployment_mcp` | `mcp/tools/rollback.py` | 배포 롤백 |
| `query_metrics_mcp` | `mcp/tools/monitor.py` | Prometheus 메트릭 쿼리 |
| `check_system_health_mcp` | `mcp/tools/health_monitor_tools.py` | 시스템 헬스 확인 |
| `check_component_health_mcp` | `mcp/tools/health_monitor_tools.py` | 컴포넌트 헬스 확인 |
| `get_health_metrics_mcp` | `mcp/tools/health_monitor_tools.py` | 헬스 메트릭 조회 |
| `send_health_alert_mcp` | `mcp/tools/health_monitor_tools.py` | 헬스 알림 전송 |
| `send_circuit_breaker_alert_mcp` | `mcp/tools/health_monitor_tools.py` | 서킷브레이커 알림 |
| `deploy_application_mcp_tool` | `mcp/tools/git_deployment_tools.py` | MCP 네이티브 배포 |
| `rollback_deployment_mcp_tool` | `mcp/tools/git_deployment_tools.py` | MCP 롤백 |
| `get_deployment_status_mcp_tool` | `mcp/tools/git_deployment_tools.py` | 배포 상태 조회 |
| `git_workflow_automation_mcp` | `mcp/tools/git_deployment_tools.py` | Git 워크플로우 자동화 |
| `create_release_tag_mcp` | `mcp/tools/git_deployment_tools.py` | 릴리스 태그 생성 |
| `list_git_agents_mcp` | `mcp/tools/git_deployment_tools.py` | Git 에이전트 목록 |

---

## 3. 인증/인가

### 3.1 JWT 인증

**파일**: `app/services/security.py`

- **알고리즘**: HS256
- **시크릿 키**: `KLEPAAS_SECRET_KEY` 환경변수 (기본값: `"your-secret-key-here"`)
- **만료 시간**: 24시간
- **토큰 위치**: `Authorization: Bearer {token}` 헤더
- **클레임**: `sub` (사용자 ID), `exp` (만료시간)

```python
# security.py - JWT 토큰 디코딩
def get_current_user_id(credentials) -> Optional[str]:
    payload = jwt.decode(token, JWT_SECRET, algorithms=["HS256"])
    return payload.get("sub")
```

**에러 처리**:
- `jwt.ExpiredSignatureError` → None 반환 (만료)
- `jwt.InvalidTokenError` → None 반환 (무효)

### 3.2 OAuth2 지원

**파일**: `app/api/v1/oauth2.py`

| 제공자 | 환경변수 | 인증 URL |
|--------|---------|---------|
| Google | `KLEPAAS_GOOGLE_CLIENT_ID/SECRET` | `https://accounts.google.com/o/oauth2/v2/auth` |
| GitHub | `KLEPAAS_GITHUB_CLIENT_ID/SECRET` | `https://github.com/login/oauth/authorize` |
| Slack | `KLEPAAS_SLACK_CLIENT_ID/SECRET` | Slack OAuth API |

**인증 흐름**:
1. 인증 코드 교환 (`exchange_google_code`, `exchange_github_code`)
2. 사용자 정보 조회 (`verify_google_token`, `verify_github_token`)
3. JWT 토큰 생성 (`create_jwt_token`)

### 3.3 Scope 기반 권한

**파일**: `app/services/security.py:58-78`

```python
# Scope 추출 (X-Scopes 헤더)
def get_token_scopes(request: Request) -> List[str]:
    header = request.headers.get("X-Scopes", "")
    return [s.strip() for s in header.split(",") if s.strip()]

# Scope 검증
def require_scopes(required: Iterable[str]):
    # 부족한 scope 있으면 HTTP 403 반환
    raise HTTPException(status_code=403, detail=f"insufficient_scope: missing {missing}")
```

### 3.4 Admin 로그인

**파일**: `app/api/v1/oauth2.py:337-375`

```python
# 하드코딩된 Admin 자격증명
@router.post("/admin/login")
async def admin_login(request: AdminLoginRequest):
    # username: "admin", password: "klepaas"
```

### 3.5 GitHub App 인증

**파일**: `app/services/cicd.py`

- HMAC-SHA256 서명 검증 (`x-hub-signature-256` 헤더)
- GitHub App 우선 검증 → PAT 폴백
- Staging 웹훅: 서명 + 타임스탬프 검증 (5분 윈도우, 재생 공격 방지)

---

## 4. 빌드/배포 파이프라인 (NCP 연동)

### 핵심 파일

- `app/services/ncp_pipeline.py` (1500+ 줄) - NCP API 통합 전체
- `app/api/v1/ncp_pipeline_api.py` - NCP Pipeline REST API
- `app/services/cicd.py` - GitHub Webhook 처리
- `app/api/v1/cicd.py` - Webhook 엔드포인트
- `app/services/ncp_deployment_status_poller.py` - 배포 상태 폴링

### GitHub → NCP 배포 흐름

```
1. GitHub Push 이벤트
   └─ POST /api/v1/cicd/webhook
   └─ HMAC-SHA256 서명 검증
   └─ PR Merge 커밋만 필터링 ("Merge pull request" 또는 pusher == "web-flow")

2. GitHub 소스코드 가져오기 (Git Clone 방식)
   └─ git clone https://x-access-token:{token}@github.com/{owner}/{repo}.git {temp_dir}
   └─ 임시 디렉토리: /tmp/git-mirror-{uuid}

3. Manifest 수정
   └─ {temp_dir}/k8s/deployment.yaml 생성/수정
   └─ 이미지 태그 및 Replicas 업데이트

4. NCP SourceCommit에 Push
   └─ git remote add sc https://{user}:{pass}@{sc_endpoint}
   └─ git push sc main --force

5. NCP SourceBuild 실행
   └─ POST https://sourcebuild.apigw.ntruss.com/api/v1/project (프로젝트 생성)
   └─ POST .../project/{id}/build (빌드 실행)
   └─ NCR 이미지명: {owner}-{repo} (소문자, 하이픈 유지)
   └─ 빌드 상태 폴링: 30초 간격 x 60회 (최대 30분)

6. NCR 이미지 검증
   └─ Docker Registry v2 API (Manifest HEAD 요청)
   └─ 401 시 Bearer token 요청 후 재시도

7. NCP SourceDeploy 실행
   └─ 배포 상태 폴링: 10초 간격 x 30회 (최대 5분)

8. DB 업데이트
   └─ deployment_histories 테이블 상태 업데이트
   └─ Slack 알림 발송

9. 정리
   └─ shutil.rmtree(temp_dir) - 임시 디렉토리 삭제
```

### NCP Object Storage

- **직접 업로드 로직 없음**
- Docker 이미지는 **NCP Container Registry(NCR)** 에 저장
- 레지스트리 URL: `{registry_project}.kr.ncr.ntruss.com`
- K8s imagePullSecrets: `ncp-cr`

### NCP API 인증

모든 NCP API 호출에 HMAC-SHA256 서명 사용:
```
x-ncp-apigw-timestamp: 현재 타임스탬프
x-ncp-iam-access-key: NCP 액세스 키
x-ncp-apigw-signature-v2: HMAC-SHA256 서명
```

### NCP SourcePipeline 자동 생성

**함수**: `ensure_sourcepipeline_project()` (ncp_pipeline.py:743-826)

```
Task 1: SourceBuild (항상 실행)
   ↓ (성공 시)
Task 2: SourceDeploy (Task 1 성공 시만 실행)

자동 트리거: SourceCommit Push 시
```

### Fallback 메커니즘

Pipeline 없거나 실패 시 → `perform_deploy()` 직접 배포로 자동 전환

---

## 5. 데이터 모델 (11개 테이블)

### 테이블 목록

| # | 테이블명 | 모델 파일 (`app/models/`) | 주요 용도 |
|---|---------|--------------------------|---------|
| 1 | `audit_logs` | `audit_log.py` | 감사 로그 (15개 필드, 4개 복합 인덱스) |
| 2 | `command_history` | `command_history.py` | NLP 명령 이력 (10개 필드) |
| 3 | `deployment_configs` | `deployment_config.py` | 배포 구성 (replica_count 등) |
| 4 | `deployment_histories` | `deployment_history.py` | 배포 이력 (35+ 필드, 단계별 상태) |
| 5 | `deployment_urls` | `deployment_url.py` | 배포 URL 매핑 |
| 6 | `notifications` | `notification.py` | 모니터링 알림 |
| 7 | `notification_reports` | `notification.py` | 알림 스냅샷 보고서 |
| 8 | `oauth_tokens` | `oauth_token.py` | OAuth 액세스 토큰 |
| 9 | `user_project_integrations` | `user_project_integration.py` | GitHub-NCP 매핑 (25+ 필드) |
| 10 | `user_repositories` | `user_repository.py` | 사용자 리포지토리 |
| 11 | `user_slack_configs` | `user_slack_config.py` | Slack 설정 (OAuth/Webhook) |

### 상세 필드 정의

#### 5.1 `audit_logs` - 감사 로그

```python
# app/models/audit_log.py
class AuditLogModel(Base):
    __tablename__ = "audit_logs"

    id              = Column(Integer, primary_key=True, index=True)
    timestamp       = Column(DateTime, nullable=False, index=True)
    user_id         = Column(String(255), nullable=False, index=True)
    user_type       = Column(String(50), nullable=False)       # user, serviceaccount, system
    source_ip       = Column(String(45), nullable=False, index=True)  # IPv4/IPv6
    user_agent      = Column(String(500), nullable=True)
    action          = Column(String(50), nullable=False, index=True)   # CREATE, UPDATE, DELETE 등
    resource_type   = Column(String(50), nullable=False, index=True)   # DEPLOYMENT, POD 등
    resource_name   = Column(String(255), nullable=True, index=True)
    namespace       = Column(String(255), nullable=True, index=True)
    result          = Column(String(20), nullable=False, index=True)   # SUCCESS, FAILURE 등
    reason          = Column(String(500), nullable=True)
    message         = Column(Text, nullable=True)
    request_body    = Column(JSON, nullable=True)
    response_body   = Column(JSON, nullable=True)
    extra_metadata  = Column(JSON, nullable=True)
```

**복합 인덱스**:
- `idx_audit_user_timestamp` (user_id, timestamp)
- `idx_audit_resource_timestamp` (resource_type, resource_name, timestamp)
- `idx_audit_action_result` (action, result, timestamp)
- `idx_audit_namespace_timestamp` (namespace, timestamp)

**Enum 타입**:
- `AuditAction`: CREATE, UPDATE, DELETE, GET, LIST, PATCH, ROLLBACK, DEPLOY, MONITOR, AUTHENTICATE, AUTHORIZE
- `AuditResource`: DEPLOYMENT, SERVICE, CONFIGMAP, SECRET, POD, EVENT, USER, SERVICE_ACCOUNT, ROLE, ROLEBINDING, NAMESPACE, CLUSTER
- `AuditResult`: SUCCESS, FAILURE, ERROR, UNAUTHORIZED, FORBIDDEN

#### 5.2 `command_history` - NLP 명령 이력

```python
# app/models/command_history.py
class CommandHistory(Base):
    __tablename__ = "command_history"

    id              = Column(Integer, primary_key=True, index=True)
    command_text    = Column(Text, nullable=False)          # 사용자 입력 명령어
    tool            = Column(String(100), nullable=False)   # 실행된 도구명
    args            = Column(JSON, nullable=True)           # 명령어 인자
    result          = Column(JSON, nullable=True)           # 실행 결과
    status          = Column(String(20), nullable=False)    # success, error, pending
    error_message   = Column(Text, nullable=True)
    user_id         = Column(String(100), nullable=True)
    created_at      = Column(DateTime, default=KST)
    updated_at      = Column(DateTime, default=KST, onupdate=KST)
```

#### 5.3 `deployment_configs` - 배포 구성

```python
# app/models/deployment_config.py
class DeploymentConfig(Base):
    __tablename__ = "deployment_configs"

    id              = Column(Integer, primary_key=True, index=True)
    github_owner    = Column(String, nullable=False, index=True)
    github_repo     = Column(String, nullable=False, index=True)
    replica_count   = Column(Integer, default=1, nullable=False)
    last_scaled_at  = Column(DateTime, nullable=True)
    last_scaled_by  = Column(String, nullable=True)
    created_at      = Column(DateTime, default=KST, nullable=False)
    updated_at      = Column(DateTime, default=KST, onupdate=KST, nullable=False)

    # 유니크 제약: uq_owner_repo (github_owner, github_repo)
```

#### 5.4 `deployment_histories` - 배포 이력 (핵심 모델)

```python
# app/models/deployment_history.py
class DeploymentHistory(Base):
    __tablename__ = "deployment_histories"

    id                      = Column(Integer, primary_key=True, index=True)
    user_id                 = Column(String(255), nullable=False, index=True)

    # GitHub 정보
    github_owner            = Column(String(255), nullable=False)
    github_repo             = Column(String(255), nullable=False)
    github_commit_sha       = Column(String(255), nullable=True)
    github_commit_message   = Column(Text, nullable=True)
    github_commit_author    = Column(String(255), nullable=True)
    github_commit_url       = Column(String(500), nullable=True)

    # NCP 파이프라인 정보
    sourcecommit_project_id = Column(String(255), nullable=True)
    sourcecommit_repo_name  = Column(String(255), nullable=True)
    sourcebuild_project_id  = Column(String(255), nullable=True)
    sourcedeploy_project_id = Column(String(255), nullable=True)
    pipeline_id             = Column(String(255), nullable=True)
    pipeline_history_id     = Column(Integer, nullable=True)
    build_id                = Column(String(255), nullable=True)
    deploy_id               = Column(String(255), nullable=True)

    # 단계별 상태
    status                  = Column(String(50), default="running", nullable=False)
    sourcecommit_status     = Column(String(50), nullable=True)
    sourcebuild_status      = Column(String(50), nullable=True)
    sourcedeploy_status     = Column(String(50), nullable=True)

    # 이미지 정보
    image_name              = Column(String(500), nullable=True)
    image_tag               = Column(String(100), nullable=True)
    image_url               = Column(String(500), nullable=True)

    # 클러스터 정보
    cluster_id              = Column(String(255), nullable=True)
    cluster_name            = Column(String(255), nullable=True)
    namespace               = Column(String(255), default="default", nullable=True)

    # 배포 구성
    replica_count           = Column(Integer, default=1, nullable=True)

    # 시간 정보 (KST)
    started_at              = Column(DateTime, default=KST, nullable=False)
    completed_at            = Column(DateTime, nullable=True)
    deployed_at             = Column(DateTime, nullable=True)
    created_at              = Column(DateTime, default=KST)
    updated_at              = Column(DateTime, default=KST, onupdate=KST)

    # 소요 시간 (초)
    total_duration          = Column(Integer, nullable=True)
    sourcecommit_duration   = Column(Integer, nullable=True)
    sourcebuild_duration    = Column(Integer, nullable=True)
    sourcedeploy_duration   = Column(Integer, nullable=True)

    # 에러 정보
    error_message           = Column(Text, nullable=True)
    error_stage             = Column(String(50), nullable=True)

    # 메타데이터
    webhook_payload         = Column(Text, nullable=True)
    auto_deploy_enabled     = Column(Boolean, default=True)

    # 롤백 정보
    is_rollback             = Column(Boolean, default=False, nullable=False)
    rollback_from_id        = Column(Integer, nullable=True)

    # 작업 유형
    operation_type          = Column(String(50), default="deploy", nullable=True)  # deploy, rollback, scale
```

**Enum 타입**:
- `DeploymentStatus`: PENDING, RUNNING, SUCCESS, FAILED, ROLLED_BACK

#### 5.5 `deployment_urls` - 배포 URL

```python
# app/models/deployment_url.py
class DeploymentUrl(Base):
    __tablename__ = "deployment_urls"

    id               = Column(Integer, primary_key=True, index=True)
    user_id          = Column(String(255), nullable=False, index=True)
    github_owner     = Column(String(255), nullable=False, index=True)
    github_repo      = Column(String(255), nullable=False, index=True)
    url              = Column(String(500), nullable=False)
    is_user_modified = Column(Boolean, default=False, nullable=False)
    created_at       = Column(DateTime, default=UTC)
    updated_at       = Column(DateTime, default=UTC, onupdate=UTC)

    # 유니크 제약: uq_user_repo_url (user_id, github_owner, github_repo)
```

#### 5.6 `notifications` - 모니터링 알림

```python
# app/models/notification.py
class Notification(Base):
    __tablename__ = "notifications"

    id          = Column(String(255), primary_key=True, index=True)
    title       = Column(String(500), nullable=False)
    description = Column(Text, nullable=True)
    severity    = Column(String(50), nullable=False, index=True)  # critical, warning, info
    source      = Column(String(255), nullable=True)               # Prometheus, Kubernetes 등
    status      = Column(String(50), nullable=False, default="firing", index=True)
    labels      = Column(JSON, nullable=True)
    created_at  = Column(DateTime, default=KST, index=True)
    resolved_at = Column(DateTime, nullable=True)

    # 복합 인덱스: idx_notification_severity_status (severity, status)
```

#### 5.7 `notification_reports` - 알림 스냅샷 보고서

```python
# app/models/notification.py
class NotificationReport(Base):
    __tablename__ = "notification_reports"

    id              = Column(String(255), primary_key=True, index=True)
    notification_id = Column(String(255), ForeignKey("notifications.id", ondelete="CASCADE"),
                             nullable=False, index=True)
    cluster         = Column(String(255), nullable=True, index=True)
    summary         = Column(Text, nullable=True)
    snapshot_json   = Column(JSON, nullable=False)
    created_at      = Column(DateTime, default=KST, index=True)

    # 외래키 관계: notification_id → notifications.id (CASCADE)
```

#### 5.8 `oauth_tokens` - OAuth 토큰

```python
# app/models/oauth_token.py
class OAuthToken(Base):
    __tablename__ = "oauth_tokens"

    id           = Column(Integer, primary_key=True, index=True)
    user_id      = Column(String(255), nullable=False, index=True)
    provider     = Column(String(50), nullable=False, index=True)
    access_token = Column(String(2048), nullable=False)
    created_at   = Column(DateTime(timezone=True), server_default=func.now())
    updated_at   = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())
```

#### 5.9 `user_project_integrations` - GitHub-NCP 매핑

```python
# app/models/user_project_integration.py
class UserProjectIntegration(Base):
    __tablename__ = "user_project_integrations"

    id                      = Column(Integer, primary_key=True, index=True)
    user_id                 = Column(String(255), nullable=False, index=True)
    user_email              = Column(String(255), nullable=True)

    # GitHub 정보
    github_owner            = Column(String(255), nullable=False)
    github_repo             = Column(String(255), nullable=False)
    github_full_name        = Column(String(512), nullable=False)
    github_repository_id    = Column(String(255), nullable=True)
    github_installation_id  = Column(String(255), nullable=True)
    github_webhook_secret   = Column(String(255), nullable=True)

    # SourceCommit 매핑
    sc_project_id           = Column(String(255), nullable=True)
    sc_repo_name            = Column(String(255), nullable=True)
    sc_clone_url            = Column(String(1024), nullable=True)
    sc_repo_id              = Column(String(255), nullable=True)

    # Build/Deploy 식별자
    build_project_id        = Column(String(255), nullable=True)
    deploy_project_id       = Column(String(255), nullable=True)
    pipeline_id             = Column(String(255), nullable=True)

    # 레지스트리 정보
    registry_url            = Column(String(512), nullable=True)
    image_repository        = Column(String(512), nullable=True)

    # 자동화 옵션
    branch                  = Column(String(100), default="main", nullable=True)
    auto_deploy_enabled     = Column(Boolean, default=True)

    # 메타데이터
    notes                   = Column(Text, nullable=True)
    created_at              = Column(DateTime(timezone=True), server_default=func.now())
    updated_at              = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())
```

#### 5.10 `user_repositories` - 사용자 리포지토리

```python
# app/models/user_repository.py
class UserRepository(Base):
    __tablename__ = "user_repositories"

    id                   = Column(Integer, primary_key=True, index=True)
    user_id              = Column(String(255), nullable=False, index=True)
    user_email           = Column(String(255), nullable=True)
    repository_owner     = Column(String(255), nullable=False)
    repository_name      = Column(String(255), nullable=False)
    repository_full_name = Column(String(255), nullable=False)
    repository_id        = Column(String(255), nullable=False)
    branch               = Column(String(100), default="main", nullable=True)
    last_sync            = Column(DateTime(timezone=True), nullable=True)
    status               = Column(String(50), default="healthy", nullable=False)
    auto_deploy_enabled  = Column(Boolean, default=False)
    webhook_configured   = Column(Boolean, default=True)
    installation_id      = Column(String(255), nullable=True)
    created_at           = Column(DateTime(timezone=True), server_default=func.now())
    updated_at           = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())
```

#### 5.11 `user_slack_configs` - Slack 설정

```python
# app/models/user_slack_config.py
class UserSlackConfig(Base):
    __tablename__ = "user_slack_configs"

    id                   = Column(Integer, primary_key=True, index=True)
    user_id              = Column(String(255), nullable=False, index=True)
    integration_type     = Column(String(20), default="oauth", nullable=False)  # oauth, webhook
    access_token         = Column(Text, nullable=True)        # Bot 토큰 (xoxb-)
    webhook_url          = Column(Text, nullable=True)        # 수신 웹훅 URL
    default_channel      = Column(String(255), nullable=True)
    deployment_channel   = Column(String(255), nullable=True)
    error_channel        = Column(String(255), nullable=True)
    dm_enabled           = Column(Boolean, default=True, nullable=False)
    dm_user_id           = Column(String(255), nullable=True)  # Slack 사용자 ID (U123...)
    created_at           = Column(DateTime(timezone=True), default=UTC)
    updated_at           = Column(DateTime(timezone=True), default=UTC, onupdate=UTC)
```

### Java Entity 존재 여부

**없음** - 전체 프로젝트가 Python(백엔드) + TypeScript(프론트엔드)로만 구성. Java 코드 없음.

### 마이그레이션 (Alembic)

| 마이그레이션 | 설명 |
|-------------|------|
| `001_add_deployment_config.py` | `deployment_configs` 테이블 생성, `deployment_histories`에 `replica_count` 추가 |
| `002_add_operation_type.py` | `deployment_histories`에 `operation_type` 컬럼 추가 |
| `003_add_notifications.py` | `notifications`, `notification_reports` 테이블 생성 |

### DB 설정 (`app/database.py`)

```python
DATABASE_URL = settings.database_url or "sqlite:////data/klepaas.db"

# SQLite 최적화
- WAL 모드 활성화 (동시성 개선)
- busy_timeout: 30초
- synchronous: NORMAL
- 연결 timeout: 30초
```

### ER 다이어그램 (관계)

```
notifications ─────< notification_reports  (1:N, CASCADE)
    │
    └── notification_id (FK)

# 나머지 테이블은 외래키 없이 user_id, github_owner/repo로 논리적 연결
```

---

## 6. 프론트엔드 연동

### 기술 스택

| 구분 | 기술 | 버전 |
|------|------|------|
| 프레임워크 | Next.js (App Router) | 15.5.6 |
| UI 라이브러리 | React | 19 |
| 타입 시스템 | TypeScript | 5 |
| 스타일링 | TailwindCSS | 4.1.9 |
| UI 컴포넌트 | Radix UI (30+ 컴포넌트) + shadcn/ui | - |
| 폼 관리 | react-hook-form | 7.60.0 |
| 검증 | Zod | 3.25.67 |
| 차트 | Recharts | latest |
| 아이콘 | Lucide React | - |
| 테마 | next-themes (다크모드) | - |
| 토스트 | Sonner | - |
| 드로어 | Vaul | - |

### 디렉토리 구조

```
frontend/
├── app/                          # Next.js App Router
│   ├── auth/                     # 인증 페이지
│   ├── oauth2-callback/          # OAuth2 콜백 페이지
│   ├── page.tsx                  # 메인 대시보드
│   ├── layout.tsx                # 루트 레이아웃
│   └── globals.css               # 전역 스타일
├── components/                   # React 컴포넌트
│   ├── dashboard-overview.tsx    # 대시보드 개요
│   ├── natural-language-command/ # 자연어 명령 입력
│   ├── deployment-status-monitoring/ # 배포 모니터링
│   ├── github-integration-panel/ # GitHub 통합
│   ├── real-time-monitoring-dashboard/ # 실시간 모니터링
│   ├── settings/                 # 설정 페이지
│   └── ui/                       # shadcn UI 기본 컴포넌트
├── contexts/
│   └── auth-context.tsx          # 인증 컨텍스트 (토큰, 사용자 관리)
├── hooks/                        # 커스텀 React 훅
├── lib/
│   ├── api.ts                    # API 클라이언트 (19KB)
│   ├── config.ts                 # 환경 설정
│   ├── types/                    # TypeScript 타입 정의
│   └── utils/                    # 유틸리티 함수
└── public/                       # 정적 자산
```

### API 클라이언트 (`frontend/lib/api.ts`)

```typescript
class ApiClient {
    // 인증 토큰 관리
    // - LocalStorage에서 'auth_token' 읽기
    // - Authorization: Bearer {token} 헤더 자동 추가

    // 사용자 필터링
    // - GET: 쿼리 파라미터로 user_id 추가
    // - POST/PUT/DELETE: 요청 본문에 user_id 추가

    // 주요 메서드
    loginWithOAuth2(provider, code, redirectUri)  // POST /api/v1/auth/oauth2/login
    getDeployments(user_id?)                      // GET  /api/v1/deployments
    sendNLPCommand(command, context?)             // POST /api/v1/nlp/command
    getGithubRepos()                              // GET  /api/v1/github/repositories
    getDeploymentStatus(deploymentId)             // GET  /api/v1/deployments/{id}/status
}
```

### API 응답 형식 (Response DTO)

FastAPI 자동 직렬화 (Pydantic 모델 또는 dict):

```json
// 성공 응답 (일반적 패턴)
{
    "status": "success",
    "data": { ... }
}

// 에러 응답
{
    "error": "에러 유형",
    "code": "에러 코드",
    "message": "에러 메시지",
    "path": "/api/v1/..."
}
```

### CORS 설정 (`app/main.py:128-135`)

```python
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],         # 모든 출처 허용 (개발용)
    allow_credentials=True,       # 인증정보 포함 요청 허용
    allow_methods=["*"],          # 모든 HTTP 메서드 허용
    allow_headers=["*"],          # 모든 헤더 허용
)
```

### 프론트엔드 환경 변수

**`.env.local`**:
```
NEXT_PUBLIC_API_URL=http://localhost:8000
NEXT_PUBLIC_WS_URL=ws://localhost:8000
```

**`lib/config.ts`**:
```typescript
export const config = {
    api: {
        baseUrl: process.env.NEXT_PUBLIC_API_URL || 'https://klepaas.com',
        wsUrl: process.env.NEXT_PUBLIC_WS_URL || 'wss://klepaas.com',
    }
}
```

### 주요 뷰/페이지

| 뷰 | 설명 |
|----|------|
| `dashboard` | 대시보드 개요 |
| `commands` | 자연어 명령 입력 |
| `deployments` | 배포 상태 모니터링 |
| `github` | GitHub 통합 패널 |
| `monitoring` | 실시간 모니터링 대시보드 |
| `settings` | 설정 |

---

## 7. 환경변수 전체 목록

### 설정 파일: `app/core/config.py` (Pydantic BaseSettings, `KLEPAAS_` 접두사)

#### AI/NLP 관련

| 변수명 | 기본값 | 용도 |
|--------|--------|------|
| `KLEPAAS_CLAUDE_API_KEY` | None | Anthropic Claude API 키 |
| `KLEPAAS_OPENAI_API_KEY` | None | OpenAI GPT-4 API 키 |
| `KLEPAAS_GEMINI_API_KEY` | None | Google Gemini API 키 |
| `KLEPAAS_GCP_PROJECT` | None | GCP 프로젝트 ID |
| `KLEPAAS_GCP_LOCATION` | "europe-west4" | GCP 리전 |
| `KLEPAAS_GEMINI_MODEL` | "gemini-2.0-flash" | Gemini 모델 선택 |
| `KLEPAAS_ADVANCED_NLP_ENABLED` | true | 고급 NLP 기능 활성화 |
| `KLEPAAS_REDIS_URL` | "redis://localhost:6379" | Redis 연결 (NLP 컨텍스트) |
| `KLEPAAS_CONTEXT_TTL` | 3600 | 컨텍스트 TTL (초) |
| `KLEPAAS_CONVERSATION_TTL` | 86400 | 대화 히스토리 TTL (초) |
| `KLEPAAS_PATTERN_TTL` | 604800 | 패턴 데이터 TTL (초) |
| `KLEPAAS_LEARNING_TTL` | 2592000 | 학습 데이터 TTL (초) |
| `KLEPAAS_MULTI_MODEL_ENABLED` | true | 다중 모델 처리 활성화 |
| `KLEPAAS_MODEL_SELECTION_STRATEGY` | "confidence_based" | 모델 선택 전략 |
| `KLEPAAS_CONFIDENCE_THRESHOLD` | 0.7 | 신뢰도 임계값 |

#### GitHub 관련

| 변수명 | 용도 |
|--------|------|
| `KLEPAAS_GITHUB_CLIENT_ID` | GitHub OAuth 클라이언트 ID |
| `KLEPAAS_GITHUB_CLIENT_SECRET` | GitHub OAuth 클라이언트 시크릿 |
| `KLEPAAS_GITHUB_APP_ID` | GitHub App ID |
| `KLEPAAS_GITHUB_APP_PRIVATE_KEY` | GitHub App 프라이빗 키 (PEM) |
| `KLEPAAS_GITHUB_APP_PRIVATE_KEY_FILE` | 프라이빗 키 파일 경로 |
| `KLEPAAS_GITHUB_APP_WEBHOOK_SECRET` | GitHub App 웹훅 시크릿 |
| `KLEPAAS_GITHUB_WEBHOOK_SECRET` | 일반 웹훅 시크릿 |
| `KLEPAAS_STAGING_WEBHOOK_SECRET` | 스테이징 웹훅 시크릿 |
| `KLEPAAS_DEPLOYMENT_CONFIG_REPO` | 배포 설정 리포 (owner/repo) |
| `KLEPAAS_DEPLOYMENT_CONFIG_TOKEN` | 배포 설정 리포 토큰 |
| `KLEPAAS_DEPLOYMENT_CONFIG_INSTALLATION_ID` | GitHub App 설치 ID |

#### NCP 관련

| 변수명 | 용도 |
|--------|------|
| `KLEPAAS_NCP_ACCESS_KEY` | NCP IAM 액세스 키 |
| `KLEPAAS_NCP_SECRET_KEY` | NCP IAM 시크릿 키 |
| `KLEPAAS_NCP_API_GW` | NCP API GW URL (기본: https://ncloud.apigw.ntruss.com) |
| `KLEPAAS_NCP_REGION` | NCP 리전 (기본: "KR") |
| `KLEPAAS_NCP_SOURCECOMMIT_ENDPOINT` | SourceCommit API 엔드포인트 |
| `KLEPAAS_NCP_SOURCECOMMIT_USERNAME` | SourceCommit Git 사용자명 |
| `KLEPAAS_NCP_SOURCECOMMIT_PASSWORD` | SourceCommit Git 비밀번호 |
| `KLEPAAS_NCP_SOURCECOMMIT_PROJECT_ID` | SourceCommit 프로젝트 ID |
| `KLEPAAS_NCP_SOURCEDEPLOY_ENDPOINT` | SourceDeploy API 엔드포인트 |
| `KLEPAAS_NCP_CONTAINER_REGISTRY_URL` | NCR URL |
| `KLEPAAS_NCP_NKS_CLUSTER_ID` | NKS 클러스터 ID |
| `KLEPAAS_NCP_NKS_CLUSTER_NO` | NKS 클러스터 번호 |

#### Google OAuth 관련

| 변수명 | 용도 |
|--------|------|
| `KLEPAAS_GOOGLE_CLIENT_ID` | Google OAuth 클라이언트 ID |
| `KLEPAAS_GOOGLE_CLIENT_SECRET` | Google OAuth 클라이언트 시크릿 |

#### Slack 관련

| 변수명 | 용도 |
|--------|------|
| `KLEPAAS_SLACK_WEBHOOK_URL` | Slack 수신 웹훅 URL |
| `KLEPAAS_SLACK_CLIENT_ID` | Slack OAuth 클라이언트 ID |
| `KLEPAAS_SLACK_CLIENT_SECRET` | Slack OAuth 클라이언트 시크릿 |
| `KLEPAAS_SLACK_REDIRECT_URI` | Slack OAuth 리디렉션 URI |
| `KLEPAAS_SLACK_ALERT_CHANNEL_DEFAULT` | 기본 알림 채널 |
| `KLEPAAS_SLACK_ALERT_CHANNEL_RATE_LIMITED` | 레이트 리밋 알림 채널 |
| `KLEPAAS_SLACK_ALERT_CHANNEL_UNAUTHORIZED` | 인증 실패 알림 채널 |

#### Kubernetes 관련

| 변수명 | 기본값 | 용도 |
|--------|--------|------|
| `KLEPAAS_ENABLE_K8S_DEPLOY` | false | K8s 배포 활성화 |
| `KLEPAAS_K8S_STAGING_NAMESPACE` | "staging" | 스테이징 네임스페이스 |
| `KLEPAAS_K8S_IMAGE_PULL_SECRET` | "ncp-cr" | 이미지 풀 시크릿 |
| `KLEPAAS_K8S_CONFIG_FILE` | None | Kubeconfig 파일 경로 |
| `KLEPAAS_K8S_CONTEXT` | None | 사용할 K8s context |

#### 모니터링 관련

| 변수명 | 용도 |
|--------|------|
| `KLEPAAS_PROMETHEUS_BASE_URL` | Prometheus 서버 URL |
| `KLEPAAS_ALERTMANAGER_URL` | AlertManager URL |
| `KLEPAAS_ALERTMANAGER_WEBHOOK_URL` | AlertManager 웹훅 URL |

#### 시스템 관련

| 변수명 | 기본값 | 용도 |
|--------|--------|------|
| `KLEPAAS_SECRET_KEY` | "your-secret-key-here" | JWT 시크릿 키 |
| `KLEPAAS_FRONTEND_URL` | "http://localhost:3000" | 프론트엔드 URL |
| `KLEPAAS_BACKEND_URL` | "http://localhost:8000" | 백엔드 URL |
| `KLEPAAS_DATABASE_URL` | "sqlite:////data/klepaas.db" | DB 연결 문자열 |
| `KLEPAAS_MCP_GIT_AGENT_ENABLED` | true | MCP Git 에이전트 활성화 |
| `KLEPAAS_MCP_DEFAULT_CLOUD_PROVIDER` | "gcp" | 기본 클라우드 제공자 (gcp/ncp) |
| `KLEPAAS_GCP_GIT_AGENT_URL` | "http://gcp-git-agent:8001" | GCP Git 에이전트 URL |
| `KLEPAAS_NCP_GIT_AGENT_URL` | "http://ncp-git-agent:8001" | NCP Git 에이전트 URL |

---

## 8. 에러 처리

### 8.1 글로벌 에러 핸들러

**파일**: `app/core/error_handler.py`

#### ErrorHandlerMiddleware (라인 17-67)

미들웨어 기반 통합 에러 처리:

| 예외 타입 | 상태코드 | 처리 |
|----------|---------|------|
| `MCPExternalError` | 400 | MCP 외부 에러 → 에러 코드, 메시지 반환 |
| `HTTPException` | 원본 유지 | HTTP 예외 → detail 반환 |
| `Exception` (기타) | 500 | 일반 예외 → "An unexpected error occurred" |

#### setup_error_handlers() (라인 70-124)

글로벌 예외 핸들러 등록:
- `@app.exception_handler(MCPExternalError)`
- `@app.exception_handler(HTTPException)`
- `@app.exception_handler(Exception)`

#### 에러 응답 형식

```json
{
    "error": "에러 유형 (MCP External Error / HTTP Exception / Internal Server Error)",
    "code": "정규화된 에러 코드 (MCPExternalError만)",
    "message": "에러 메시지",
    "path": "/api/v1/..."
}
```

### 8.2 커스텀 예외 클래스

#### MCPExternalError (`app/mcp/external/errors.py:22-35`)

```python
class MCPExternalError(Exception):
    code: ErrorCode           # 정규화된 에러 코드
    message: str              # 에러 메시지
    retry_after_seconds: Optional[float]  # 재시도 대기 시간
    details: Optional[dict]   # 추가 상세정보
```

**ErrorCode 종류** (9가지):

| 코드 | HTTP 상태 | 설명 |
|------|---------|------|
| `unauthorized` | 401 | 인증 실패 |
| `forbidden` | 403 | 권한 부족 |
| `not_found` | 404 | 리소스 없음 |
| `rate_limited` | 429 | 속도 제한 |
| `timeout` | 504 | 타임아웃 |
| `unavailable` | 503 | 서비스 불가 |
| `bad_request` | 400 | 잘못된 요청 |
| `conflict` | 409 | 충돌 |
| `internal` | 500 | 내부 에러 |

#### ApiException (`app/services/ncp_pipeline.py:16-17, 52-53`)

NCP SDK 미설치 시 폴백용 임시 예외 클래스

### 8.3 서킷 브레이커 패턴

**파일**: `app/mcp/external/handlers.py`

#### 설정 (MCPHandlerConfig)

```python
timeout: float = 30.0                    # 요청 타임아웃
max_retries: int = 3                     # 최대 재시도
retry_delay: float = 1.0                 # 재시도 대기시간
breaker_failure_threshold: int = 5       # 실패 임계값
breaker_reset_timeout_sec: float = 30.0  # 회로 재설정 시간
```

#### 동작 흐름

```
정상 상태 (Closed)
    ↓ (5회 연속 실패)
회로 오픈 (Open) → MCPExternalError("circuit_open")
    ↓ (30초 경과)
Half-Open → 다음 요청으로 테스트
    ├─ 성공 → 정상 (Closed)
    └─ 실패 → 다시 오픈 (Open)
```

#### Slack 알림 연동

에러 코드별 채널 라우팅:
- `rate_limited` → `KLEPAAS_SLACK_ALERT_CHANNEL_RATE_LIMITED`
- `unauthorized` → `KLEPAAS_SLACK_ALERT_CHANNEL_UNAUTHORIZED`
- 기타 → `KLEPAAS_SLACK_ALERT_CHANNEL_DEFAULT`

알림은 **non-blocking** (실패해도 계속 진행)

### 8.4 재시도 로직

**파일**: `app/mcp/external/retry.py`

#### retry_async() 함수

```python
# 지수 백오프 + 지터(Jitter)
attempts: int = 5              # 총 시도 횟수
base_delay: float = 0.2        # 기본 대기 시간
max_delay: float = 5.0         # 최대 대기 시간
jitter: float = 0.2            # 지터 비율 (0.0~1.0)

# 대기 시간 계산
delay = min(base_delay * (2 ** (attempt - 1)), max_delay)
delta = delay * jitter
delay = max(0.0, delay + random.uniform(-delta, delta))

# 서버 제시 retry_after 존중
if exc.retry_after_seconds:
    delay = max(delay, float(exc.retry_after_seconds))
```

#### 재시도 가능 에러 코드

- `rate_limited` (429)
- `timeout` (504)
- `unavailable` (503)
- `internal` (500)

### 8.5 Prometheus 메트릭

**파일**: `app/mcp/external/metrics.py`

| 메트릭 | 타입 | 레이블 | 설명 |
|--------|------|--------|------|
| `MCP_EXTERNAL_REQUESTS` | Counter | provider, operation, result | 외부 MCP 요청 총수 |
| `MCP_EXTERNAL_LATENCY` | Histogram | provider, operation | 요청 지연시간 (버킷: 0.05~5.0초) |
| `MCP_EXTERNAL_ERRORS` | Counter | provider, operation, code | 에러 총수 |
| `MCP_EXTERNAL_HEALTH` | Gauge | provider | 헬스 상태 (1=정상, 0=장애) |

### 8.6 Pydantic 검증 에러

FastAPI 자동 처리:
- Pydantic 모델이 요청 바디 자동 검증
- 검증 실패 시 **422 Unprocessable Entity** 응답
- FastAPI가 자동으로 에러 상세 정보 생성

### 8.7 에러 처리 전체 흐름

```
클라이언트 요청
    ↓
ErrorHandlerMiddleware.dispatch()
    ↓
라우터 → 서비스 로직
    ↓
[예외 발생]
    ├─ MCPExternalError
    │   ├─ 상태코드 400
    │   ├─ 에러 로깅
    │   ├─ Slack 알림 (non-blocking)
    │   └─ JSON 응답 {error, code, message, path}
    │
    ├─ HTTPException
    │   ├─ 상태코드 유지
    │   ├─ 경고 로깅
    │   └─ JSON 응답 {error, message, path}
    │
    └─ Exception (기타)
        ├─ 상태코드 500
        ├─ 에러 로깅 + 스택트레이스
        └─ JSON 응답 {error: "Internal Server Error", message, path}
```

---

## 9. 보안 권고사항

| 우선순위 | 항목 | 현재 상태 | 권장 조치 |
|---------|------|---------|---------|
| **Critical** | JWT 시크릿 키 | `"your-secret-key-here"` (기본값) | `openssl rand -hex 32`로 강력한 난수 생성 |
| **Critical** | Admin 비밀번호 | 하드코딩 `"klepaas"` (`oauth2.py:341`) | 환경변수 `KLEPAAS_ADMIN_PASSWORD`로 이동 |
| **Critical** | CORS 설정 | `allow_origins=["*"]` | 프로덕션에서 특정 도메인으로 제한 |
| **High** | `.env` 파일 | 실제 자격증명 포함 | Secret Manager (Vault, AWS SM 등) 사용 |
| **High** | Refresh Token | 없음 (24시간 고정 만료) | Refresh Token 메커니즘 추가 |
| **Medium** | Rate Limiting | 없음 | API별 Rate Limit 추가 (특히 인증 엔드포인트) |
| **Medium** | 에러 추적 ID | 없음 | Correlation ID 추가 (로그-응답 연결) |
| **Low** | Pydantic 검증 | 기본 FastAPI 처리 | 커스텀 핸들러로 일관된 형식 |
| **Low** | API 에러 문서화 | 없음 | OpenAPI 스키마에 에러 응답 예시 추가 |

---

## 부록: Docker Compose 통합 테스트 환경

**파일**: `integration-tests/docker-compose.yml`

| 서비스 | 포트 | 자격증명 |
|--------|------|---------|
| PostgreSQL | 5433→5432 | postgres / postgres123 |
| RabbitMQ | 5672, 15672 | admin / admin123 |
| Prometheus | 9090 | - |
| Alertmanager | 9093 | - |
| Grafana | 3001→3000 | admin / admin123 |
| Backend | 8000→8080 | - |
| PostgreSQL Bridge | 8001 | - |
| RabbitMQ Bridge | 8002 | - |
| Prometheus Bridge | 8003 | - |

---

> 이 문서는 Claude Code에 의해 자동 생성되었습니다.
