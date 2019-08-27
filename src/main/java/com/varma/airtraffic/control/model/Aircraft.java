package com.varma.airtraffic.control.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Aircraft {
    private String aircraftId;
    private AircraftType aircraftType;
    private AircraftSize aircraftSize;
    private AircraftSpecialFlag aircraftSpecialFlag;
    private String priorityId;
    private String airportCode;
    private String arrivalTime;
    private String departureTime;
}
