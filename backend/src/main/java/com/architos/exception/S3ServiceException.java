package com.architos.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
public class S3ServiceException extends RuntimeException {

    public S3ServiceException(String message) {
        super(message);
    }

    public S3ServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}