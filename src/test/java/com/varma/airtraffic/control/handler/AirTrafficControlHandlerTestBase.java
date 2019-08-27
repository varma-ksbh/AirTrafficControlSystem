package com.varma.airtraffic.control.handler;

import com.varma.airtraffic.control.config.AirTrafficControlTestComponent;
import com.varma.airtraffic.control.config.DaggerAirTrafficControlTestComponent;
import org.junit.After;
import org.junit.Before;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

import javax.inject.Inject;

/**
 * This class serves as the base class for Integration tests. do not include IT in
 * the class name so that it does not get picked up by failsafe.
 */
public abstract class AirTrafficControlHandlerTestBase {
    private static final String TABLE_NAME = "AircraftTable";

    private final AirTrafficControlTestComponent atcComponent;

    @Inject
    DynamoDbClient dynamoDb;

    public AirTrafficControlHandlerTestBase() {
        atcComponent = DaggerAirTrafficControlTestComponent.builder().build();
        atcComponent.inject(this);
    }

    @Before
    public void setup() {
        dynamoDb.createTable(CreateTableRequest.builder()
                .tableName(TABLE_NAME)
                .keySchema(KeySchemaElement.builder()
                        .keyType(KeyType.HASH)
                        .attributeName("aircraftId")
                        .build())
                .attributeDefinitions(
                        AttributeDefinition.builder()
                                .attributeName("aircraftId")
                                .attributeType(ScalarAttributeType.S)
                                .build())
                .provisionedThroughput(
                        ProvisionedThroughput.builder()
                                .readCapacityUnits(1L)
                                .writeCapacityUnits(1L)
                                .build())
                .build());

    }

    @After
    public void teardown() {
        dynamoDb.deleteTable(DeleteTableRequest.builder().tableName(TABLE_NAME).build());
    }
}
