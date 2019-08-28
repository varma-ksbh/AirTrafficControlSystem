package com.varma.airtraffic.control.handler;

import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.varma.airtraffic.control.model.response.ErrorMessage;
import com.varma.airtraffic.control.model.response.GatewayResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Map;

public interface AircraftRequestStreamHandler extends RequestStreamHandler {
    int SC_OK = 200;
    int SC_CREATED = 201;
    int SC_BAD_REQUEST = 400;
    int SC_NOT_FOUND = 404;
    int SC_CONFLICT = 409;
    int SC_INTERNAL_SERVER_ERROR = 500;
    Map<String, String> APPLICATION_JSON = Collections.singletonMap("Content-Type",
            "application/json");
    ErrorMessage REQUEST_WAS_NULL_ERROR
            = new ErrorMessage("Request was null", SC_BAD_REQUEST);
    ErrorMessage AIRCRAFT_ID_WAS_NOT_SET
            = new ErrorMessage("Aircraft with given aircraftId is NOT_FOUND", SC_NOT_FOUND);
    ErrorMessage AIRPORT_CODE_WAS_NOT_SET
            = new ErrorMessage("Airport with given code is NOT_FOUND", SC_NOT_FOUND);

    /**
     * This method writes a body has invalid JSON response.
     * @param objectMapper the mapper to use for converting the error response to JSON.
     * @param output the output stream to write with the mapper.
     * @param details a detailed message describing why the JSON was invalid.
     * @throws IOException if there was an issue converting the ErrorMessage object to JSON.
     */
    default void writeInvalidJsonInStreamResponse(ObjectMapper objectMapper,
                                                  OutputStream output,
                                                  String details) throws IOException {
        objectMapper.writeValue(output, new GatewayResponse<>(
                objectMapper.writeValueAsString(new ErrorMessage("Invalid JSON in body: "
                        + details, SC_BAD_REQUEST)),
                APPLICATION_JSON, SC_BAD_REQUEST));
    }

    default boolean isNullOrEmpty(final String string) {
        return string == null || string.isEmpty();
    }

    default boolean isNullOrEmpty(final Enum tEnum) {
        return tEnum == null || tEnum.name().isEmpty();
    }
}
