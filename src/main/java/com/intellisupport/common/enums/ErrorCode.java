package com.intellisupport.common.enums;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Standardized application error codes, associated HTTP statuses, and default messages.
 */
@Getter
public enum ErrorCode {

    // Common errors
    INTERNAL_SERVER_ERROR("ERR_INTERNAL_SERVER_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected internal server error occurred"),
    BAD_REQUEST("ERR_BAD_REQUEST", HttpStatus.BAD_REQUEST, "The request is invalid or malformed"),
    VALIDATION_FAILED("ERR_VALIDATION_FAILED", HttpStatus.BAD_REQUEST, "Validation failed for one or more fields"),
    METHOD_NOT_ALLOWED("ERR_METHOD_NOT_ALLOWED", HttpStatus.METHOD_NOT_ALLOWED, "HTTP method not supported for this endpoint"),

    // Auth & Security errors
    UNAUTHORIZED("ERR_UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "Full authentication is required to access this resource"),
    INVALID_CREDENTIALS("ERR_INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "Invalid email or password"),
    TOKEN_EXPIRED("ERR_TOKEN_EXPIRED", HttpStatus.UNAUTHORIZED, "Authentication token has expired"),
    TOKEN_INVALID("ERR_TOKEN_INVALID", HttpStatus.UNAUTHORIZED, "Authentication token is invalid"),
    FORBIDDEN("ERR_FORBIDDEN", HttpStatus.FORBIDDEN, "You do not have permission to perform this action"),
    ACCESS_DENIED("ERR_ACCESS_DENIED", HttpStatus.FORBIDDEN, "Access denied"),

    // Resource errors
    RESOURCE_NOT_FOUND("ERR_RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND, "The requested resource was not found"),
    USER_NOT_FOUND("ERR_USER_NOT_FOUND", HttpStatus.NOT_FOUND, "User not found"),
    TICKET_NOT_FOUND("ERR_TICKET_NOT_FOUND", HttpStatus.NOT_FOUND, "Ticket not found"),
    ARTICLE_NOT_FOUND("ERR_ARTICLE_NOT_FOUND", HttpStatus.NOT_FOUND, "Knowledge article not found"),
    RESOURCE_ALREADY_EXISTS("ERR_RESOURCE_ALREADY_EXISTS", HttpStatus.CONFLICT, "Resource already exists"),
    EMAIL_ALREADY_EXISTS("ERR_EMAIL_ALREADY_EXISTS", HttpStatus.CONFLICT, "Email address is already registered"),

    // Business domain errors
    INVALID_STATUS_TRANSITION("ERR_INVALID_STATUS_TRANSITION", HttpStatus.BAD_REQUEST, "Invalid ticket status transition"),
    TICKET_ALREADY_ASSIGNED("ERR_TICKET_ALREADY_ASSIGNED", HttpStatus.BAD_REQUEST, "Ticket is already assigned to this agent"),
    AI_SERVICE_ERROR("ERR_AI_SERVICE_ERROR", HttpStatus.SERVICE_UNAVAILABLE, "AI service is currently unavailable or failed to generate a response");

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(String code, HttpStatus httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }
}
