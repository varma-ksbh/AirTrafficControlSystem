package com.varma.airtraffic.control.exception;

public class AircraftDoesNotExistException extends IllegalArgumentException {
    public AircraftDoesNotExistException(String message) {
        super(message);
    }
}
