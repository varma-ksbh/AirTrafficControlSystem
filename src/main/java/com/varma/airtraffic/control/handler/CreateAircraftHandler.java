package com.varma.airtraffic.control.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.varma.airtraffic.control.config.AirTrafficControlComponent;
import com.varma.airtraffic.control.config.DaggerAirTrafficControlComponent;
import com.varma.airtraffic.control.dao.AircraftDao;
import com.varma.airtraffic.control.dao.PriorityAircraftsDao;
import com.varma.airtraffic.control.exception.CouldNotCreateAircraftException;
import com.varma.airtraffic.control.model.Aircraft;
import com.varma.airtraffic.control.model.request.CreateAircraftPriorityRequest;
import com.varma.airtraffic.control.model.request.CreateAircraftRequest;
import com.varma.airtraffic.control.model.request.UpdateAirportPriorityRequest;
import com.varma.airtraffic.control.model.response.ErrorMessage;
import com.varma.airtraffic.control.model.response.GatewayResponse;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class CreateAircraftHandler implements AircraftRequestStreamHandler {
    private static final ErrorMessage REQUIRE_AIRPORT_CODE
            = new ErrorMessage("Require airportCode to create an airplane entry", SC_BAD_REQUEST);

    private static final ErrorMessage REQUIRE_AC_TYPE
            = new ErrorMessage("Require aircraftType to create an airplane entry", SC_BAD_REQUEST);

    private static final ErrorMessage REQUIRE_AC_SIZE
            = new ErrorMessage("Require aircraftSize to create an airplane entry", SC_BAD_REQUEST);


    @Inject
    ObjectMapper objectMapper;
    @Inject
    AircraftDao acDao;
    @Inject
    PriorityAircraftsDao priorityAcDao;

    private final AirTrafficControlComponent acComponent;

    public CreateAircraftHandler() {
        acComponent = DaggerAirTrafficControlComponent.builder().build();
        acComponent.inject(this);
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
        JsonNode createAircraftRequestBody = event.findValue("body");
        if (createAircraftRequestBody == null) {
            objectMapper.writeValue(output,
                    new GatewayResponse<>(
                            objectMapper.writeValueAsString(
                                    new ErrorMessage("Body was null",
                                            SC_BAD_REQUEST)),
                            APPLICATION_JSON, SC_BAD_REQUEST));
            return;
        }
        final CreateAircraftRequest request;
        try {
            request = objectMapper.treeToValue(
                    objectMapper.readTree(createAircraftRequestBody.asText()),
                    CreateAircraftRequest.class);
        } catch (JsonParseException | JsonMappingException e) {
            objectMapper.writeValue(output,
                    new GatewayResponse<>(
                            objectMapper.writeValueAsString(
                                    new ErrorMessage("Invalid JSON in body: "
                                            + e.getMessage(), SC_BAD_REQUEST)),
                            APPLICATION_JSON, SC_BAD_REQUEST));
            return;
        }

        if (request == null) {
            objectMapper.writeValue(output,
                    new GatewayResponse<>(
                            objectMapper.writeValueAsString(REQUEST_WAS_NULL_ERROR),
                            APPLICATION_JSON, SC_BAD_REQUEST));
            return;
        }

        if (isNullOrEmpty(request.getAirportCode())) {
            objectMapper.writeValue(output,
                    new GatewayResponse<>(
                            objectMapper.writeValueAsString(REQUIRE_AIRPORT_CODE),
                            APPLICATION_JSON, SC_BAD_REQUEST));
            return;
        }
        if (isNullOrEmpty(request.getAircraftSize())) {
            objectMapper.writeValue(output,
                    new GatewayResponse<>(
                            objectMapper.writeValueAsString(REQUIRE_AC_SIZE),
                            APPLICATION_JSON, SC_BAD_REQUEST));
            return;
        }
        if (isNullOrEmpty(request.getAircraftType())) {
            objectMapper.writeValue(output,
                    new GatewayResponse<>(
                            objectMapper.writeValueAsString(REQUIRE_AC_TYPE),
                            APPLICATION_JSON, SC_BAD_REQUEST));
            return;
        }
        try {
            final Aircraft ac = acDao.createAircraft(request);
            // Below should be implemented as stream with dead letter queues and should be alarmed for failures
            priorityAcDao.createAircraftPriority(CreateAircraftPriorityRequest.builder()
                    .aircraftId(ac.getAircraftId())
                    .priorityId(ac.getPriorityId())
                    .arrivalTime(ac.getArrivalTime())
                    .build());
            priorityAcDao.updateAirportPriorityQueue(UpdateAirportPriorityRequest.builder()
                    .priorityId(ac.getPriorityId())
                    .airportCode(ac.getAirportCode())
                    .date(ac.getArrivalTime()).build());
            objectMapper.writeValue(output,
                    new GatewayResponse<>(objectMapper.writeValueAsString(ac),
                            APPLICATION_JSON, SC_CREATED));
        } catch (CouldNotCreateAircraftException e) {
            objectMapper.writeValue(output,
                    new GatewayResponse<>(
                            objectMapper.writeValueAsString(
                                    new ErrorMessage(e.getMessage(),
                                            SC_INTERNAL_SERVER_ERROR)),
                            APPLICATION_JSON, SC_INTERNAL_SERVER_ERROR));
        }
    }
}
