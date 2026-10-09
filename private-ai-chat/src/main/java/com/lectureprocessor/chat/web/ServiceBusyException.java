package com.lectureprocessor.chat.web;

/**
 * Raised when the single generation worker and its bounded queue are saturated.
 */
public class ServiceBusyException extends RuntimeException {

    public ServiceBusyException(String message) {
        super(message);
    }
}
