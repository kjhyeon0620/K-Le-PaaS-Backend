package klepaas.backend.auth.token.exception;

import klepaas.backend.global.exception.BusinessException;
import klepaas.backend.global.exception.ErrorCode;

public class CliAccessTokenNotFoundException extends BusinessException {

    public CliAccessTokenNotFoundException() {
        super(ErrorCode.CLI_TOKEN_NOT_FOUND);
    }
}
