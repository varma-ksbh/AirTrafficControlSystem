package com.varma.airtraffic.control.exception;

public class AirportDoesNotExistException extends IllegalArgumentException {
    public AirportDoesNotExistException(String message) {
        super(message);
    }
}
