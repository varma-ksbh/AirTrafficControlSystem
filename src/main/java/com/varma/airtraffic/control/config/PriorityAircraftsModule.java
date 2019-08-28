package com.varma.airtraffic.control.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.varma.airtraffic.control.dao.PriorityAircraftsDao;
import dagger.Module;
import dagger.Provides;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import javax.inject.Named;
import javax.inject.Singleton;

@Module
public class PriorityAircraftsModule {
    @Singleton
    @Provides
    public PriorityAircraftsDao priorityAirCraftDao(DynamoDbClient dynamoDb,
                                                    @Named("priorityAircraftTableName") String tableName) {
        return new PriorityAircraftsDao(dynamoDb, tableName);
    }
}
