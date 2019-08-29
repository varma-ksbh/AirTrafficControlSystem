package com.varma.airtraffic.control.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.varma.airtraffic.control.config.AirTrafficControlComponent;
import com.varma.airtraffic.control.config.DaggerAirTrafficControlComponent;
import com.varma.airtraffic.control.dao.AircraftDao;
import com.varma.airtraffic.control.dao.PriorityAircraftsDao;
import com.varma.airtraffic.control.exception.AircraftDoesNotExistException;
import com.varma.airtraffic.control.exception.AirportWithEmptyAircraftsException;
import com.varma.airtraffic.control.exception.UnableToDeleteException;
import com.varma.airtraffic.control.exception.UnableToUpdateException;
import com.varma.airtraffic.control.model.Aircraft;
import com.varma.airtraffic.control.model.AircraftPriority;
import com.varma.airtraffic.control.model.AirportPriority;
import com.varma.airtraffic.control.model.response.ErrorMessage;
import com.varma.airtraffic.control.model.response.GatewayResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

public class DequeueAircraftHandler implements AircraftRequestStreamHandler {
    @Inject
    ObjectMapper objectMapper;
    @Inject
    AircraftDao aircraftDao;
    @Inject
    PriorityAircraftsDao priorityAircraftsDao;

    private final AirTrafficControlComponent atcComponent;
    private final Log logger = LogFactory.getLog(DequeueAircraftHandler.class);

    public DequeueAircraftHandler() {
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
        final String airportCode = Optional.ofNullable(pathParameterMap)
                .map(mapNode -> mapNode.get("airportCode"))
                .map(JsonNode::asText)
                .orElse(null);
        if (isNullOrEmpty(airportCode)) {
            objectMapper.writeValue(output,
                    new GatewayResponse<>(
                            objectMapper.writeValueAsString(AIRPORT_CODE_WAS_NOT_SET),
                            APPLICATION_JSON, SC_BAD_REQUEST));
            return;
        }
        String errorMessage = null;
        try {
            final AirportPriority apEntry = priorityAircraftsDao.getHighestPriorityIdForAirport(airportCode);
            logger.debug("Airport" + airportCode + "has the highest priority" + apEntry.getPriorityId());
            final AircraftPriority acEntry = priorityAircraftsDao.getOldestAircraftIdWithPriorityId(apEntry.getPriorityId());
            logger.debug("Aircraft with highest priority to be dequeue is:" + acEntry.getAircraftId());

            final Aircraft aircraft = aircraftDao.deleteAircraft(acEntry.getAircraftId());

            priorityAircraftsDao.deletePriorityAircraft(acEntry);
            if (aircraft.getArrivalTime().equals(apEntry.getDate())) {
                priorityAircraftsDao.deleteAirportPriorityEntry(apEntry);
            }

            objectMapper.writeValue(output,
                    new GatewayResponse<>(
                            objectMapper.writeValueAsString(aircraft),
                            APPLICATION_JSON, SC_OK));
        } catch (AircraftDoesNotExistException e) {
            errorMessage = e.getMessage();
        } catch (AirportWithEmptyAircraftsException e) {
            errorMessage = e.getMessage();
        } catch (UnableToDeleteException e) {
            errorMessage = e.getMessage();
        } catch (UnableToUpdateException e) {
            errorMessage = e.getMessage();
        }
        objectMapper.writeValue(output,
                new GatewayResponse<>(
                        objectMapper.writeValueAsString(
                                new ErrorMessage(errorMessage, SC_NOT_FOUND)),
                        APPLICATION_JSON, SC_NOT_FOUND));
    }
}