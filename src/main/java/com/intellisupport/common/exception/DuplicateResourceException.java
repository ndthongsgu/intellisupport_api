package com.intellisupport.common.exception;

import com.intellisupport.common.enums.ErrorCode;

/**
 * Thrown when attempting to create or update an entity that conflicts with an existing one (e.g. duplicate email).
 */
public class DuplicateResourceException extends AppException {

    public DuplicateResourceException(String message) {
        super(ErrorCode.RESOURCE_ALREADY_EXISTS, message);
    }

    public DuplicateResourceException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(ErrorCode.RESOURCE_ALREADY_EXISTS,
                String.format("%s already exists with %s: '%s'", resourceName, fieldName, fieldValue));
    }
}
