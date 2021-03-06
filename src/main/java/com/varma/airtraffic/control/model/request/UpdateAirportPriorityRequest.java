package com.varma.airtraffic.control.model.request;

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
public class UpdateAirportPriorityRequest {
    private String airportCode;
    private String priorityId;
    private String date;
}
