package com.varma.airtraffic.control.dao;

import com.varma.airtraffic.control.exception.TableDoesNotExistException;
import com.varma.airtraffic.control.model.AirportPriorityQueueEntry;
import com.varma.airtraffic.control.model.PriorityAircraft;
import com.varma.airtraffic.control.model.request.CreateAircraftPriorityRequest;
import com.varma.airtraffic.control.model.request.UpdateAirportPriorityQueueRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

import java.util.HashMap;
import java.util.Map;

public class PriorityAircraftsDao {
    private static final String PRIORITY_ID = "hashKey";
    private static final String AIRCRAFT_ID = "rangeKey";
    private static final String AIRPORT_CODE = "hashKey";
    private static final String AIRPORT_PRIORITY_QUEUE_ENTRY = "rangeKey";

    private final String tableName;
    private final DynamoDbClient dynamoDb;

    public PriorityAircraftsDao(final DynamoDbClient dynamoDb, final String tableName) {
        this.dynamoDb = dynamoDb;
        this.tableName = tableName;
    }

    private Map<String, AttributeValue> createAircraftPriorityItem(final CreateAircraftPriorityRequest request) {
        Map<String, AttributeValue> result = new HashMap<>();
        result.put(PRIORITY_ID, AttributeValue.builder()
                .s(request.getPriorityId())
                .build());
        result.put(AIRCRAFT_ID, AttributeValue.builder()
                .s(request.getAircraftId())
                .build());
        result.put("date", AttributeValue.builder()
                .s(request.getArrivalTime())
                .build());
        return result;
    }

    private Map<String, AttributeValue> updateAirportPriorityItem(final UpdateAirportPriorityQueueRequest request) {
        Map<String, AttributeValue> result = new HashMap<>();
        result.put(AIRPORT_CODE, AttributeValue.builder()
                .s(request.getAirportCode())
                .build());
        result.put(AIRPORT_PRIORITY_QUEUE_ENTRY, AttributeValue.builder()
                .s(request.getPriorityId())
                .build());
        result.put("date", AttributeValue.builder()
                .s(request.getDate())
                .build());
        return result;
    }

    public PriorityAircraft createAircraftPriority(CreateAircraftPriorityRequest request) {
        final Map<String, AttributeValue> item = createAircraftPriorityItem(request);
        try {
            dynamoDb.putItem(PutItemRequest.builder()
                    .tableName(tableName)
                    .item(item)
                    .build()).itemCollectionMetrics();
        } catch (ResourceNotFoundException e) {
            throw new TableDoesNotExistException(
                    "PrioritiesAircraft table " + tableName + " does not exist");
        }
        return PriorityAircraft.builder()
                .aircraftId(request.getAircraftId())
                .priorityId(request.getPriorityId())
                .arrivalTime(request.getArrivalTime())
                .build();
    }

    public AirportPriorityQueueEntry updateAirportPriorityQueue(UpdateAirportPriorityQueueRequest request) {
        final Map<String, AttributeValue> item = updateAirportPriorityItem(request);
        try {
            dynamoDb.putItem(PutItemRequest.builder()
                    .tableName(tableName)
                    .item(item)
                    .build()).itemCollectionMetrics();
        } catch (ResourceNotFoundException e) {
            throw new TableDoesNotExistException(
                    "PrioritiesAircraft table " + tableName + " does not exist");
        }
        return AirportPriorityQueueEntry.builder()
                .airportCode(request.getAirportCode())
                .priorityId(request.getPriorityId())
                .date(request.getDate())
                .build();
    }
}
