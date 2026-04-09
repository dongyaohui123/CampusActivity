package com.campus.activity.common;

import org.springframework.http.HttpStatus;

/**
 * Unified error code definition for API responses.
 */
public enum ErrorCode {
    SUCCESS(0, HttpStatus.OK, "success"),
    BAD_REQUEST(40000, HttpStatus.BAD_REQUEST, "bad request"),
    VALIDATION_ERROR(40001, HttpStatus.BAD_REQUEST, "validation failed"),
    FORBIDDEN(40300, HttpStatus.FORBIDDEN, "forbidden"),
    NOT_FOUND(40400, HttpStatus.NOT_FOUND, "resource not found"),
    CONFLICT(40900, HttpStatus.CONFLICT, "data conflict"),
    DATA_INTEGRITY_ERROR(40901, HttpStatus.CONFLICT, "data integrity violation"),
    INTERNAL_ERROR(50000, HttpStatus.INTERNAL_SERVER_ERROR, "internal server error");

    private final int code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(int code, HttpStatus httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public int getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
