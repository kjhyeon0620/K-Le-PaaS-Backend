# 🤝 Contributing Guide

## 🌱 브랜치 전략 (Git Flow)
이 프로젝트는 **Git Flow** 전략을 따릅니다.



[Image of Git Flow diagram]


- **main**: 프로덕션 배포용 브랜치 (직접 커밋 금지, PR 병합만 허용)
- **develop**: 다음 릴리즈를 위한 개발 통합 브랜치
- **feature/***: 새로운 기능 개발 브랜치 (`develop`에서 분기)
- **fix/***: 개발 중 발견된 버그 수정 브랜치 (`develop`에서 분기)
- **hotfix/***: 운영 이슈 긴급 수정 브랜치 (`main`에서 분기)
- **release/***: 릴리즈 준비 및 QA 브랜치 (`develop`에서 분기)

## 📍 브랜치 네이밍 컨벤션

| 브랜치 유형 | 설명 | 예시 |
|:---:|---|---|
| **main** | 배포 브랜치 | `main` |
| **develop** | 개발 브랜치 | `develop` |
| **feature** | 기능 개발 | `feature/#3-user-login` |
| **fix** | 버그 수정 | `fix/#12-ncp-connection-error` |
| **hotfix** | 긴급 수정 | `hotfix/#20-security-patch` |
| **refactor** | 리팩토링 | `refactor/#5-api-structure` |

> **Note:** 브랜치명 뒤에는 관련된 **Issue 번호**를 명시하여 추적을 용이하게 합니다.

## 💬 커밋 메시지 컨벤션 (Conventional Commits)
**형식:** `type: subject` (영문 소문자 권장, 한글 혼용 가능)

| Type | 설명 |
|:---:|---|
| **feat** | 새로운 기능 추가 |
| **fix** | 버그 수정 |
| **docs** | 문서 수정 (README, 주석 등) |
| **style** | 코드 포맷팅, 세미콜론 누락 등 (로직 변경 없음) |
| **refactor** | 코드 리팩토링 (기능 변경 없음) |
| **test** | 테스트 코드 추가/수정 |
| **chore** | 빌드 태스크, 패키지 매니저 설정 등 |
| **ci** | CI 설정 파일 수정 |
| **perf** | 성능 개선 |

**✅ 예시**
- `feat: NCP 배포 로직 Strategy Pattern 적용 (#1)`
- `fix: JWT 만료 시간 버그 수정 (#3)`
- `docs: README 아키텍처 다이어그램 업데이트`

## 🛠 PR (Pull Request) 가이드

1. **대상 브랜치 확인:**
   - `feature` → `develop`
   - `hotfix` → `main` & `develop`
2. **PR 템플릿 작성:** 제공된 템플릿에 맞춰 '변경 사항'과 '테스트 결과'를 상세히 작성합니다.
3. **이슈 연결:** PR 본문 또는 제목에 `Closes #이슈번호`를 적어 이슈를 자동으로 닫습니다.
4. **리뷰 및 병합:** (1인 프로젝트인 경우) 스스로 코드 리뷰를 진행한 후, **Squash and Merge**를 권장합니다.

## 📚 코드 스타일 및 기타

- **Java:** IntelliJ Default Formatter 또는 Google Java Format을 준수합니다.
- **Python:** Black, isort를 사용합니다.
- **Test:** 핵심 비즈니스 로직에 대해서는 반드시 단위 테스트(JUnit/Pytest)를 작성합니다.
