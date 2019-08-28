package com.varma.airtraffic.control.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.varma.airtraffic.control.exception.CouldNotCreateAircraftException;
import com.varma.airtraffic.control.exception.TableDoesNotExistException;
import com.varma.airtraffic.control.model.Aircraft;
import com.varma.airtraffic.control.model.AircraftSize;
import com.varma.airtraffic.control.model.AircraftSpecialFlag;
import com.varma.airtraffic.control.model.AircraftType;
import com.varma.airtraffic.control.model.request.CreateAircraftRequest;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

public class AircraftDaoTest {
    private static final String AIRCRAFT_ID = "some ac id";
    private DynamoDbClient dynamoDb = mock(DynamoDbClient.class);
    private PriorityAircraftsDao priorityAircraftsDao = mock(PriorityAircraftsDao.class);
    private ObjectMapper mapper = new ObjectMapper();
    private AircraftDao aircraftDao = new AircraftDao(dynamoDb, "table_name", priorityAircraftsDao);

    private CreateAircraftRequest createAircraftRequest = CreateAircraftRequest.builder()
            .aircraftSize(AircraftSize.LARGE)
            .aircraftType(AircraftType.CARGO)
            .airportCode("IAD")
            .aircraftSpecialFlag(AircraftSpecialFlag.EMERGENCY)
            .build();


    @Test(expected = IllegalArgumentException.class)
    public void createAircraft_whenRequestNull_throwsIllegalArgumentException() {
        aircraftDao.createAircraft(null);
    }

    @Test(expected = TableDoesNotExistException.class)
    public void createAircraft_whenTableDoesNotExist_throwsTableDoesNotExistException() {
        doThrow(ResourceNotFoundException.builder().build()).when(dynamoDb).putItem(any(PutItemRequest.class));
        aircraftDao.createAircraft(createAircraftRequest);
    }

    @Test(expected = TableDoesNotExistException.class)
    public void getAircraft_whenTableDoesNotExist_throwsTableDoesNotExistException() {
        doThrow(ResourceNotFoundException.builder().build()).when(dynamoDb).getItem(any(GetItemRequest.class));
        aircraftDao.getAircraft(AIRCRAFT_ID);
    }

    //conditional failure tests
    @Test(expected = CouldNotCreateAircraftException.class)
    public void createAircraft_whenAlreadyExists_throwsCouldNotCreateAircraftException() {
        doThrow(ConditionalCheckFailedException.builder().build()).when(dynamoDb).putItem(any(PutItemRequest.class));
        aircraftDao.createAircraft(createAircraftRequest);
    }

    //positive functional tests
    @Test
    public void createAircraft_whenAircraftDoesNotExist_createsAircraftWithPopulatedId() {
        Map<String, AttributeValue> createdItem = new HashMap<>();
        createdItem.put("aircraftId", AttributeValue.builder().s(UUID.randomUUID().toString()).build());
        createdItem.put("aircraftType", AttributeValue.builder().s(AircraftType.CARGO.name()).build());
        createdItem.put("aircraftSize", AttributeValue.builder().s(AircraftSize.LARGE.name()).build());
        createdItem.put("aircraftSize", AttributeValue.builder().s(AircraftSpecialFlag.EMERGENCY.name()).build());
        createdItem.put("airportCode", AttributeValue.builder().s("IAD").build());
        createdItem.put("priorityId", AttributeValue.builder().s("IAD-503070").build());
        doReturn(PutItemResponse.builder().attributes(createdItem).build()).when(dynamoDb).putItem(any(PutItemRequest.class));

        Aircraft ac = aircraftDao.createAircraft(createAircraftRequest);

        assertNotNull(ac.getAircraftId());
        assertEquals("IAD-503070", ac.getPriorityId());
        assertNotNull(UUID.fromString(ac.getAircraftId()));
    }
}
