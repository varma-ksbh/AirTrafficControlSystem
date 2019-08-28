package com.varma.airtraffic.control.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.varma.airtraffic.control.exception.AircraftDoesNotExistException;
import com.varma.airtraffic.control.exception.CouldNotCreateAircraftException;
import com.varma.airtraffic.control.exception.TableDoesNotExistException;
import com.varma.airtraffic.control.model.Aircraft;
import com.varma.airtraffic.control.model.AircraftSize;
import com.varma.airtraffic.control.model.AircraftSpecialFlag;
import com.varma.airtraffic.control.model.AircraftType;
import com.varma.airtraffic.control.model.request.CreateAircraftPriorityRequest;
import com.varma.airtraffic.control.model.request.CreateAircraftRequest;
import com.varma.airtraffic.control.model.request.UpdateAirportPriorityQueueRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;

public class AircraftDao {
    private static final String AIRCRAFT_ID = "aircraftId";
    private static final String AIRPORT_CODE_WAS_NULL = "airport code was null";
    private static final String AIRCRAFT_TYPE_WAS_NULL = "aircraftType or Size was null";
    private static final String AIRCRAFT_SIZE_WAS_NULL = "aircraftType or Size was null";

    private final String tableName;
    private final DynamoDbClient dynamoDb;
    private final PriorityAircraftsDao priorityAircraftsDao;

    public AircraftDao(final DynamoDbClient dynamoDb, final String tableName,
                       final PriorityAircraftsDao priorityAircraftsDao) {
        this.dynamoDb = dynamoDb;
        this.tableName = tableName;
        this.priorityAircraftsDao = priorityAircraftsDao;
    }

    /**
     * Returns an aircraft or throws if the aircraft does not exist.
     * @param aircraftId id of aircraft to get
     * @return the aircraft if it exists
     * @throws AircraftDoesNotExistException if the aircraft does not exist
     */
    public Aircraft getAircraft(final String aircraftId) {
        try {
            return Optional.ofNullable(
                    dynamoDb.getItem(GetItemRequest.builder()
                            .tableName(tableName)
                            .key(Collections.singletonMap(AIRCRAFT_ID,
                                    AttributeValue.builder().s(aircraftId).build()))
                            .build()))
                    .map(GetItemResponse::item)
                    .map(this::convert)
                    .orElseThrow(() -> new AircraftDoesNotExistException("Aircraft "
                            + aircraftId + " does not exist"));
        } catch (ResourceNotFoundException e) {
            throw new TableDoesNotExistException("Aircraft table " + tableName + " does not exist");
        }
    }

    private Aircraft convert(final Map<String, AttributeValue> item) {
        if (item == null || item.isEmpty()) {
            return null;
        }
        Aircraft.AircraftBuilder builder = Aircraft.builder();

        try {
            builder.aircraftId(item.get(AIRCRAFT_ID).s());
        } catch (NullPointerException e) {
            throw new IllegalStateException(
                    "item did not have an aircraftId attribute or it was not a String");
        }
        return builder.build();
    }

    private String getCurrentMomentAsString() {
        final TimeZone tz = TimeZone.getTimeZone("UTC");
        final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        df.setTimeZone(tz);
        return df.format(new Date());
    }

    private String calculatePriority(
            final String airportCode,
            final AircraftType type, final AircraftSize size,
                                  final Optional<AircraftSpecialFlag> specialFlags) {
        int priority = type.getValue() + size.getValue();
        if (specialFlags.isPresent()) priority += specialFlags.get().getValue();
        return String.join("-", airportCode, String.valueOf(priority));
    }

    private Map<String, AttributeValue> createAircraftItem(final CreateAircraftRequest aircraftRequest) {
        Map<String, AttributeValue> result = new HashMap<>();

        final String airportCode = aircraftRequest.getAirportCode().toUpperCase(Locale.ENGLISH);
        result.put("airportCode", AttributeValue.builder()
                    .s(airportCode)
                    .build());
        result.put(AIRCRAFT_ID, AttributeValue.builder()
                .s(UUID.randomUUID().toString())
                .build());
        result.put("priorityId", AttributeValue.builder()
                    .s(calculatePriority(airportCode, aircraftRequest.getAircraftType(), aircraftRequest.getAircraftSize(),
                            Optional.ofNullable(aircraftRequest.getAircraftSpecialFlag())))
                    .build());
        result.put("arrivalTime", AttributeValue.builder()
                .s(getCurrentMomentAsString())
                .build());
        result.put("aircraftType", AttributeValue.builder()
                .s(aircraftRequest.getAircraftType().name())
                .build());
        result.put("aircraftSize", AttributeValue.builder()
                .s(aircraftRequest.getAircraftSize().name())
                .build());
        Optional.ofNullable(aircraftRequest.getAircraftSpecialFlag()).ifPresent(specialFlag ->
                result.put("aircraftSpecialFlags", AttributeValue.builder()
                        .s(specialFlag.name())
                        .build()));

        return result;
    }

    /**
     * Creates an Aircraft.
     * @param createAircraftRequest details of Aircraft to create
     * @return created Aircraft
     */
    public Aircraft createAircraft(final CreateAircraftRequest createAircraftRequest) {
        if (createAircraftRequest == null) {
            throw new IllegalArgumentException("CreateAircraftRequest was null");
        }
        if (createAircraftRequest.getAirportCode() == null) {
            throw new IllegalArgumentException(AIRPORT_CODE_WAS_NULL);
        }
        if (createAircraftRequest.getAircraftType() == null) {
            throw new IllegalArgumentException(AIRCRAFT_TYPE_WAS_NULL);
        }

        if (createAircraftRequest.getAircraftSize() == null) {
            throw  new IllegalArgumentException(AIRCRAFT_SIZE_WAS_NULL);
        }

        int tries = 0;
        while (tries < 2) {
            try {
                Map<String, AttributeValue> item = createAircraftItem(createAircraftRequest);
                dynamoDb.putItem(PutItemRequest.builder()
                        .tableName(tableName)
                        .item(item)
                        .conditionExpression("attribute_not_exists(" + AIRCRAFT_ID + ")")
                        .build());

                // Below section will be removed pretty soon and will be implemented as db streams and make it transcational
                // Failure in the below operations should trigger alarms.

                priorityAircraftsDao.createAircraftPriority(CreateAircraftPriorityRequest.builder()
                        .aircraftId(item.get(AIRCRAFT_ID).s())
                        .priorityId(item.get("priorityId").s())
                        .arrivalTime(item.get("arrivalTime").s())
                        .build()
                );
                priorityAircraftsDao.updateAirportPriorityQueue(UpdateAirportPriorityQueueRequest.builder()
                        .priorityId(item.get("priorityId").s())
                        .airportCode(item.get("airportCode").s())
                        .date(item.get("arrivalTime").s())
                        .build());
                return Aircraft.builder()
                        .aircraftId(item.get(AIRCRAFT_ID).s())
                        .aircraftSize(AircraftSize.valueOf(item.get("aircraftSize").s()))
                        .aircraftType(AircraftType.valueOf(item.get("aircraftType").s()))
                        .priorityId(item.get("priorityId").s())
                        .arrivalTime(item.get("arrivalTime").s())
                        .airportCode(item.get("airportCode").s())
                        .build();
            } catch (ConditionalCheckFailedException e) {
                tries++;
            } catch (ResourceNotFoundException e) {
                throw new TableDoesNotExistException(
                        "Aircraft table " + tableName + " does not exist");
            }
        }
        throw new CouldNotCreateAircraftException(
                "Unable to generate aircraft after 2 tries");
    }
}
