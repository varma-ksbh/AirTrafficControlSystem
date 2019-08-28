package com.varma.airtraffic.control.config;

import com.varma.airtraffic.control.dao.AircraftDao;
import com.varma.airtraffic.control.dao.PriorityAircraftsDao;
import dagger.Module;
import dagger.Provides;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import javax.inject.Named;
import javax.inject.Singleton;

@Module
public class AircraftModule {
    @Singleton
    @Provides
    public AircraftDao airCraftDao(DynamoDbClient dynamoDb, @Named("aircraftTableName") String tableName, PriorityAircraftsDao priorityAircraftsDao) {
        return new AircraftDao(dynamoDb, tableName,priorityAircraftsDao);
    }
}
