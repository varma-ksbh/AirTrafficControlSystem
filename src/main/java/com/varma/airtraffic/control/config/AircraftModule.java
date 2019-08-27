package com.varma.airtraffic.control.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.varma.airtraffic.control.dao.AircraftDao;
import dagger.Module;
import dagger.Provides;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import javax.inject.Named;
import javax.inject.Singleton;

@Module
public class AircraftModule {
    @Singleton
    @Provides
    public AircraftDao airCraftDao(DynamoDbClient dynamoDb, @Named("aircraftTableName") String tableName,
                                   ObjectMapper mapper) {
        return new AircraftDao(dynamoDb, tableName,10, mapper);
    }
}
