package com.varma.airtraffic.control.config;

import com.varma.airtraffic.control.dao.AircraftDao;
import com.varma.airtraffic.control.handler.CreateAircraftHandler;
import com.varma.airtraffic.control.handler.GetAircraftHandler;
import dagger.Component;

import javax.inject.Singleton;

@Singleton
@Component(modules = { DataConverterModule.class, DynamoModule.class, AircraftModule.class})
public interface AirTrafficControlComponent {

    AircraftDao provideAircraftDao();

    void inject(GetAircraftHandler requestHandler);

    void inject(CreateAircraftHandler requestHandler);
}
