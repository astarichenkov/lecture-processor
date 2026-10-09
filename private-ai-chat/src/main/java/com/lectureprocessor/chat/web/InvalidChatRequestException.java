package com.lectureprocessor.chat.web;

/**
 * Raised for invalid or oversized chat requests.
 */
public class InvalidChatRequestException extends RuntimeException {

    public InvalidChatRequestException(String message) {
        super(message);
    }
}
