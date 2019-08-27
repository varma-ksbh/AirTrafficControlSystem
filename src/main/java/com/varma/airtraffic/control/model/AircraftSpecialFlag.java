package com.varma.airtraffic.control.model;

public enum AircraftSpecialFlag {
    EMERGENCY(500000), NORMAL(0);

    private final int value;

    AircraftSpecialFlag(final int newValue) {
        value = newValue;
    }

    public int getValue() {
        return value;
    }
}
