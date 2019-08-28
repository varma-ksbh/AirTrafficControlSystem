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
public class PriorityAircraft {
    private String priorityId;
    private String aircraftId;
    private String arrivalTime;
}
