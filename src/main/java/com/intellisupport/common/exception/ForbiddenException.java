package com.intellisupport.common.exception;

import com.intellisupport.common.enums.ErrorCode;

/**
 * Thrown when an authenticated user does not have permission for the requested action.
 */
public class ForbiddenException extends AppException {

    public ForbiddenException(String message) {
        super(ErrorCode.FORBIDDEN, message);
    }

    public ForbiddenException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
