package com.intellisupport.common.exception;

import com.intellisupport.common.enums.ErrorCode;

/**
 * Thrown when an unauthenticated user attempts to access a protected resource
 * or credentials supplied are invalid.
 */
public class UnauthorizedException extends AppException {

    public UnauthorizedException(String message) {
        super(ErrorCode.UNAUTHORIZED, message);
    }

    public UnauthorizedException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
