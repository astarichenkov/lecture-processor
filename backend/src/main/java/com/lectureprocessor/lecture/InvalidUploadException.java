package com.lectureprocessor.lecture;

/**
 * Raised when an uploaded file fails validation.
 */
public class InvalidUploadException extends RuntimeException {

    public InvalidUploadException(String message) {
        super(message);
    }
}
