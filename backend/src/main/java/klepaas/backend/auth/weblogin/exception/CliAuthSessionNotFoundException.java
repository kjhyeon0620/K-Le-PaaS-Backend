package klepaas.backend.auth.weblogin.exception;

import klepaas.backend.global.exception.BusinessException;
import klepaas.backend.global.exception.ErrorCode;

public class CliAuthSessionNotFoundException extends BusinessException {

    public CliAuthSessionNotFoundException() {
        super(ErrorCode.CLI_AUTH_SESSION_NOT_FOUND);
    }
}
