package com.varma.airtraffic.control.exception;

public class TableExistsException extends IllegalStateException {
    public TableExistsException(String message) {
        super(message);
    }
}
