package com.varma.airtraffic.control.model.response;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.varma.airtraffic.control.model.Aircraft;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonAutoDetect
public class GetAirCraftResponse {
    private final Aircraft airCraft;
}
