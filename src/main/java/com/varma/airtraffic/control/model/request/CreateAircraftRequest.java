package com.varma.airtraffic.control.model.request;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.varma.airtraffic.control.model.AircraftSize;
import com.varma.airtraffic.control.model.AircraftSpecialFlag;
import com.varma.airtraffic.control.model.AircraftType;
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
@JsonAutoDetect
public class CreateAircraftRequest {
    private String airportCode;
    private AircraftType aircraftType;
    private AircraftSize aircraftSize;
    private AircraftSpecialFlag aircraftSpecialFlag;
}
