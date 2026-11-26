package com.studentanalytics.dao;

/** Unchecked wrapper for database failures, so callers do not need to catch SQLException. */
public class DataAccessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}