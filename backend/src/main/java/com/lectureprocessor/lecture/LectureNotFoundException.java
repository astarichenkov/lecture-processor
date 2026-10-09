package com.lectureprocessor.lecture;

/**
 * Raised when a requested lecture does not exist.
 */
public class LectureNotFoundException extends RuntimeException {

    public LectureNotFoundException(String id) {
        super("Лекция не найдена: " + id);
    }
}
