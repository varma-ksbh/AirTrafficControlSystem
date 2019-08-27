package com.varma.airtraffic.control.exception;

public class TableDoesNotExistException extends IllegalStateException {
    public TableDoesNotExistException(String message) {
        super(message);
    }
}
