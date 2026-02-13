# Phase 4.5: Swagger + 테스트 코드 구현 계획

## Git 워크플로우
1. main 체크아웃 + pull
2. GitHub 이슈 생성 (gh CLI 설치 후)
3. `feat/#9-test-swagger` 브랜치 생성
4. 구현
5. 커밋 + 푸시 + PR 생성

## 구현 순서

### Step 1: build.gradle 의존성 추가
- `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.4.0`
- `org.springframework.boot:spring-boot-starter-test`
- `org.springframework.security:spring-security-test`

### Step 2: Swagger 설정
- `global/config/SwaggerConfig.java` 생성 (OpenAPI + JWT BearerAuth SecurityScheme)
- `SecurityConfig.java` 수정: `/swagger-ui/**`, `/v3/api-docs/**` permitAll 추가

### Step 3: 서비스 단위 테스트 (Mockito)
- `DeploymentServiceTest.java` — createDeployment, getDeployment, getDeploymentStatus
- `RepositoryServiceTest.java` — createRepository, getRepositories, deleteRepository, updateDeploymentConfig
- `AuthServiceTest.java` — login, refresh, getOAuthUrl

### Step 4: 컨트롤러 통합 테스트 (MockMvc)
- `DeploymentControllerTest.java` — POST /deployments, GET /deployments, GET /deployments/{id}/status
- `RepositoryControllerTest.java` — POST/GET/DELETE /repositories, PUT /repositories/{id}/config
- `AuthControllerTest.java` — GET /auth/oauth2/url, POST /auth/oauth2/login, POST /auth/refresh

### Step 5: 커밋 + 푸시 + PR 생성
