package com.varma.airtraffic.control.dao;

import com.varma.airtraffic.control.exception.AirportWithEmptyAircraftsException;
import com.varma.airtraffic.control.exception.TableDoesNotExistException;
import com.varma.airtraffic.control.model.AirportPriority;
import com.varma.airtraffic.control.model.AircraftPriority;
import com.varma.airtraffic.control.model.request.CreateAircraftPriorityRequest;
import com.varma.airtraffic.control.model.request.UpdateAirportPriorityRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class PriorityAircraftsDao {
    private static final String PRIORITY_ID = "hashKey";
    private static final String AIRCRAFT_ID = "rangeKey";

    private static final String AIRPORT_CODE = "hashKey";
    private static final String AIRPORT_PRIORITY_QUEUE_ENTRY = "rangeKey";

    private static final String PRIORITY_DATE_INDEX = "pDateIndex";

    private static final Log logger = LogFactory.getLog(PriorityAircraftsDao.class);

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

    private Map<String, AttributeValue> updateAirportPriorityItem(final UpdateAirportPriorityRequest request) {
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

    public AircraftPriority createAircraftPriority(CreateAircraftPriorityRequest request) {
        final Map<String, AttributeValue> item = createAircraftPriorityItem(request);
        try {
            dynamoDb.putItem(PutItemRequest.builder()
                    .tableName(tableName)
                    .item(item)
                    .build()).itemCollectionMetrics();
        } catch (ResourceNotFoundException e) {
            logger.error("PrioritiesAircraft table " + tableName + " does not exist", e);
            throw new TableDoesNotExistException(
                    "PrioritiesAircraft table " + tableName + " does not exist");
        }
        return AircraftPriority.builder()
                .aircraftId(request.getAircraftId())
                .priorityId(request.getPriorityId())
                .arrivalTime(request.getArrivalTime())
                .build();
    }

    public AirportPriority updateAirportPriorityQueue(UpdateAirportPriorityRequest request) {
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
        return AirportPriority.builder()
                .airportCode(request.getAirportCode())
                .priorityId(request.getPriorityId())
                .date(request.getDate())
                .build();
    }

    public AirportPriority getHighestPriorityIdForAirport(final String airportCode) {
        return Optional.ofNullable(queryForHighestPriorityForAirport(airportCode))
                        .map(this::convertToAirportPriority)
                        .orElseThrow(() -> new AirportWithEmptyAircraftsException("No Aircrafts exist for AirportCode:"
                                + airportCode));
    }

    public AircraftPriority getOldestAircraftIdWithPriorityId(final String priorityId) {
        return Optional.ofNullable(queryForOldestAircraftWithPriority(priorityId))
                .map(this::convertToAircraftPriority)
                .orElseThrow(() ->
                        new AirportWithEmptyAircraftsException("No Aircraft exist with PriorityCode:"
                                + priorityId));
    }

    public AircraftPriority deletePriorityAircraft(final AircraftPriority request) {
        Map<String, AttributeValue> keyExpression = new HashMap<>();
        keyExpression.put(PRIORITY_ID, AttributeValue.builder()
                .s(request.getPriorityId())
                .build());
        keyExpression.put(AIRCRAFT_ID, AttributeValue.builder()
                .s(request.getAircraftId())
                .build());
        try {
            DeleteItemResponse response = dynamoDb.deleteItem(DeleteItemRequest.builder()
                    .tableName(tableName)
                    .key(keyExpression)
                    .returnValues(ReturnValue.ALL_OLD)
                    .build());
            return convertToAircraftPriority(response.attributes());
        } catch (Exception e) {
            logger.error("Failed to delete PriorityAircraft with PriorityId(hashKey): "
                    + ", aircraftId(rangeKey): " + request.getPriorityId() + request.getAircraftId(), e);
        }
        return null;
    }

    public AirportPriority deleteAirportPriorityEntry(final AirportPriority request) {
        Map<String, AttributeValue> keyExpression = new HashMap<>();
        keyExpression.put(AIRPORT_CODE, AttributeValue.builder()
                .s(request.getAirportCode())
                .build());
        keyExpression.put(AIRPORT_PRIORITY_QUEUE_ENTRY, AttributeValue.builder()
                .s(request.getPriorityId())
                .build());
        try {
            DeleteItemResponse response = dynamoDb.deleteItem(DeleteItemRequest.builder()
                    .key(keyExpression)
                    .tableName(tableName)
                    .returnValues(ReturnValue.ALL_OLD)
                    .build());
            return convertToAirportPriority(response.attributes());
        } catch (Exception e) {
            logger.error("Failed to delete AirportPriority with AirportCode(hashKey):"
                    + request.getAirportCode() +  " , priorityId(rangeKey):" + request.getPriorityId(), e);
        }
        return null;
    }

    public Map<String, AttributeValue> queryForHighestPriorityForAirport(String airportCode) {
        final Map<String,String> expressionAttributesNames = new HashMap<>();
        expressionAttributesNames.put("#hashKey","hashKey");

        final Map<String,AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":value", AttributeValue.builder().s(airportCode).build());

        final QueryRequest queryRequest = QueryRequest.builder()
                .tableName(tableName)
                .keyConditionExpression("#hashKey = :value")
                .expressionAttributeNames(expressionAttributesNames)
                .expressionAttributeValues(expressionAttributeValues)
                .scanIndexForward(false)
                .limit(1)
                .build();
        try {
            return dynamoDb.query(queryRequest).items().get(0);
        } catch (Exception e) {
            logger.error("Failed to queryForHighestPriorityForAirport :" + airportCode, e);
        }
        return null;
    }

    public Map<String,AttributeValue> queryForOldestAircraftWithPriority(String priorityId) {
        final Map<String,String> expressionAttributesNames = new HashMap<>();
        expressionAttributesNames.put("#hashKey","hashKey");

        final Map<String,AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":value", AttributeValue.builder().s(priorityId).build());

        QueryRequest queryRequest = QueryRequest.builder()
                .tableName(tableName)
                .indexName(PRIORITY_DATE_INDEX)
                .keyConditionExpression("#hashKey = :value")
                .expressionAttributeNames(expressionAttributesNames)
                .expressionAttributeValues(expressionAttributeValues)
                .scanIndexForward(true)
                .limit(1)
                .build();

        try {
            return dynamoDb.query(queryRequest).items().get(0);
        } catch (Exception e) {
            logger.error("Failed to queryForOldestAircraftWithPriority:" + priorityId, e);
        }
        return null;
    }

    private AirportPriority convertToAirportPriority(Map<String, AttributeValue> item) {
        if (item == null || item.isEmpty()) {
            return null;
        }
        AirportPriority.AirportPriorityBuilder builder = AirportPriority.builder();

        try {
            builder.airportCode(item.get(AIRPORT_CODE).s());
            builder.priorityId(item.get(AIRPORT_PRIORITY_QUEUE_ENTRY).s());
            builder.date(item.get("date").s());
        } catch (NullPointerException e) {
            throw new IllegalStateException(
                    "item did not have an aircraftId attribute or it was not a String");
        }
        return builder.build();
    }

    private AircraftPriority convertToAircraftPriority(Map<String, AttributeValue> item) {
        if (item == null || item.isEmpty()) {
            return null;
        }
        AircraftPriority.AircraftPriorityBuilder builder = AircraftPriority.builder();

        try {
            builder.priorityId(item.get(PRIORITY_ID).s());
            builder.aircraftId(item.get(AIRCRAFT_ID).s());
            builder.arrivalTime(item.get("date").s());
        } catch (NullPointerException e) {
            throw new IllegalStateException(
                    "item did not have an aircraftId attribute or it was not a String");
        }
        return builder.build();
    }
}
