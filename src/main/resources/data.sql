-- 개발용 시드 데이터 (Phase 3 인증 구현 전까지 사용)
INSERT INTO users (id, email, name, role, provider_id, created_at, updated_at)
VALUES (1, 'dev@klepaas.io', 'Dev User', 'USER', 'github-dev-123', NOW(), NOW());
