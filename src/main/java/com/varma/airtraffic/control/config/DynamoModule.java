package com.varma.airtraffic.control.config;

import dagger.Module;
import dagger.Provides;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

import javax.inject.Named;
import javax.inject.Singleton;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Optional;

@Module
public class DynamoModule {
    @Singleton
    @Provides
    @Named("aircraftTableName")
    String aircraftTableName() {
        return Optional.ofNullable(System.getenv("AIRCRAFT_TABLE_NAME")).orElse("AircraftTable");
    }

    @Singleton
    @Provides
    @Named("priorityAircraftTableName")
    String priorityAircraftTableName() {
        return Optional.ofNullable(System.getenv("PRIORITY_AIRCRAFT_TABLE_NAME"))
                .orElse("priority_aircraft_table");
    }

    @Singleton
    @Provides
    DynamoDbClient dynamoDb() {
        String endpoint = System.getenv("ENDPOINT_OVERRIDE");

        DynamoDbClientBuilder builder = DynamoDbClient.builder();
        builder.httpClient(ApacheHttpClient.builder().build());
        if (endpoint != null && !endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder.build();
    }
}
