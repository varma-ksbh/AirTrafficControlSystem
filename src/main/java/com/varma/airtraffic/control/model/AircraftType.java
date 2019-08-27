package com.varma.airtraffic.control.model;

public enum AircraftType {
    VIP(7000), PASSENGER(5000), CARGO(3000);

    private final int value;

    AircraftType(final int newValue) {
        value = newValue;
    }

    public int getValue() {
        return value;
    }
}
