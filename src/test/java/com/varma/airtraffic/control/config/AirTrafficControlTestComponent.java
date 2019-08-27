package com.varma.airtraffic.control.config;

import com.varma.airtraffic.control.dao.AircraftDao;
import com.varma.airtraffic.control.handler.AirTrafficControlHandlerTestBase;
import dagger.Component;

import javax.inject.Singleton;

@Singleton
@Component(modules = { DataConverterModule.class, DynamoModule.class, AircraftModule.class})
public interface AirTrafficControlTestComponent {
    AircraftDao provideAircraftDao();
    void inject(AirTrafficControlHandlerTestBase integrationTest);
}
