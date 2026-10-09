package com.lectureprocessor.document;

/**
 * Raised when document generation fails.
 */
public class DocumentExportException extends RuntimeException {

    public DocumentExportException(String message) {
        super(message);
    }

    public DocumentExportException(String message, Throwable cause) {
        super(message, cause);
    }
}
