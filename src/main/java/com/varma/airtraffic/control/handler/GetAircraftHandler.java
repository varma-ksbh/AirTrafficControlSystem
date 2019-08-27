package com.varma.airtraffic.control.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.varma.airtraffic.control.config.AirTrafficControlComponent;
import com.varma.airtraffic.control.config.DaggerAirTrafficControlComponent;
import com.varma.airtraffic.control.dao.AircraftDao;
import com.varma.airtraffic.control.exception.AircraftDoesNotExistException;
import com.varma.airtraffic.control.model.Aircraft;
import com.varma.airtraffic.control.model.response.ErrorMessage;
import com.varma.airtraffic.control.model.response.GatewayResponse;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

public class GetAircraftHandler implements AircraftRequestStreamHandler {
    @Inject
    ObjectMapper objectMapper;
    @Inject
    AircraftDao aircraftDao;
    private final AirTrafficControlComponent atcComponent;

    public GetAircraftHandler() {
        atcComponent = DaggerAirTrafficControlComponent.builder().build();
        atcComponent.inject(this);
    }

    @Override
    public void handleRequest(InputStream input, OutputStream output,
                              Context context) throws IOException {
        final JsonNode event;
        try {
            event = objectMapper.readTree(input);
        } catch (JsonMappingException e) {
            writeInvalidJsonInStreamResponse(objectMapper, output, e.getMessage());
            return;
        }
        if (event == null) {
            writeInvalidJsonInStreamResponse(objectMapper, output, "event was null");
            return;
        }
        final JsonNode pathParameterMap = event.findValue("pathParameters");
        final String aircraftId = Optional.ofNullable(pathParameterMap)
                .map(mapNode -> mapNode.get("aircraftId"))
                .map(JsonNode::asText)
                .orElse(null);
        if (isNullOrEmpty(aircraftId)) {
            objectMapper.writeValue(output,
                    new GatewayResponse<>(
                            objectMapper.writeValueAsString(AIRCRAFT_ID_WAS_NOT_SET),
                            APPLICATION_JSON, SC_BAD_REQUEST));
            return;
        }
        try {
            Aircraft aircraft = aircraftDao.getAircraft(aircraftId);
            objectMapper.writeValue(output,
                    new GatewayResponse<>(
                            objectMapper.writeValueAsString(aircraft),
                            APPLICATION_JSON, SC_OK));
        } catch (AircraftDoesNotExistException e) {
            objectMapper.writeValue(output,
                    new GatewayResponse<>(
                            objectMapper.writeValueAsString(
                                    new ErrorMessage(e.getMessage(), SC_NOT_FOUND)),
                            APPLICATION_JSON, SC_NOT_FOUND));
        }
    }
}
