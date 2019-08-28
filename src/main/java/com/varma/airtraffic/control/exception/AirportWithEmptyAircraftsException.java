package com.varma.airtraffic.control.exception;

public class AirportWithEmptyAircraftsException extends IllegalStateException {
    public AirportWithEmptyAircraftsException(final String message) {
        super(message);
    }
}
