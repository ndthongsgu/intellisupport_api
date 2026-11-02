package com.intellisupport.common.exception;

import com.intellisupport.common.enums.ErrorCode;

/**
 * Thrown when an incoming request is semantically invalid or violates business constraints.
 */
public class BadRequestException extends AppException {

    public BadRequestException(String message) {
        super(ErrorCode.BAD_REQUEST, message);
    }

    public BadRequestException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
