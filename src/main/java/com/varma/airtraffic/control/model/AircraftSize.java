package com.varma.airtraffic.control.model;

public enum AircraftSize {
    LARGE(70), SMALL(30);

    private final int value;

    AircraftSize(final int newValue) {
        value = newValue;
    }

    public int getValue() {
        return value;
    }
}
