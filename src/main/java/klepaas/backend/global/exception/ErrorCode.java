package klepaas.backend.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_001", "요청한 리소스를 찾을 수 없습니다"),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "COMMON_002", "이미 존재하는 리소스입니다"),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_003", "잘못된 요청입니다"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_004", "서버 내부 오류가 발생했습니다"),

    // Repository
    REPOSITORY_NOT_FOUND(HttpStatus.NOT_FOUND, "REPO_001", "저장소를 찾을 수 없습니다"),
    REPOSITORY_ALREADY_EXISTS(HttpStatus.CONFLICT, "REPO_002", "이미 등록된 저장소입니다"),

    // Deployment
    DEPLOYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "DEPLOY_001", "배포를 찾을 수 없습니다"),
    DEPLOYMENT_CONFIG_NOT_FOUND(HttpStatus.NOT_FOUND, "DEPLOY_002", "배포 설정을 찾을 수 없습니다"),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "사용자를 찾을 수 없습니다");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
