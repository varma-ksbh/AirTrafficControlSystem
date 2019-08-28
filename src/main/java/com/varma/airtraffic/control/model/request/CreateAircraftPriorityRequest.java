package com.varma.airtraffic.control.model.request;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
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
public class CreateAircraftPriorityRequest {
    private String aircraftId;
    private String priorityId;
    private String arrivalTime;
}
