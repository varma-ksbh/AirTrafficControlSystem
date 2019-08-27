package com.varma.airtraffic.control.handler;

import com.varma.airtraffic.control.services.lambda.runtime.TestContext;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertTrue;

public class CreateAircraftHandlerTest {
    private CreateAircraftHandler handler = new CreateAircraftHandler();

    @Test
    public void handleRequest_whenCreateInputStreamEmpty_puts400InOutputStream() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        handler.handleRequest(new ByteArrayInputStream(new byte[0]), os, TestContext.builder().build());
        assertTrue(os.toString().contains("Invalid JSON"));
        assertTrue(os.toString().contains("400"));
    }

    @Test
    public void handleRequest_whenCreateInputStreamHasNoBody_puts400InOutputStream() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        String input = "{}";
        handler.handleRequest(new ByteArrayInputStream(input.getBytes()), os, TestContext.builder().build());
        assertTrue(os.toString().contains("Body was null"));
        assertTrue(os.toString().contains("400"));
    }

    @Test
    public void handleRequest_whenCreateInputStreamHasNullBody_puts400InOutputStream() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        String input = "{\"body\": \"null\"}";
        handler.handleRequest(new ByteArrayInputStream(input.getBytes()), os, TestContext.builder().build());
        assertTrue(os.toString().contains("Request was null"));
        assertTrue(os.toString().contains("400"));
    }

    @Test
    public void handleRequest_whenCreateInputStreamHasWrongTypeForBody_puts400InOutputStream() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        String input = "{\"body\": \"1\"}";
        handler.handleRequest(new ByteArrayInputStream(input.getBytes()), os, TestContext.builder().build());
        assertTrue(os.toString().contains("Invalid JSON"));
        assertTrue(os.toString().contains("400"));
    }

    @Test
    public void handleRequest_whenCreateInputStreamHasEmptyBodyDict_puts400InOutputStream() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        String input = "{\"body\": \"{}\"}";
        handler.handleRequest(new ByteArrayInputStream(input.getBytes()), os, TestContext.builder().build());

        assertTrue(os.toString().contains("Require airportCode to create an airplane entry"));
        assertTrue(os.toString().contains("400"));
    }

    @Test
    public void handleRequest_whenCreateInputStreamOnlyHasFewData_puts400InOutputStream() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        String input = "{\"body\": \"{\\\"airportCode\\\": \\\"IAD\\\"}\"}";
        handler.handleRequest(new ByteArrayInputStream(input.getBytes()), os, TestContext.builder().build());

        assertTrue(os.toString().contains("Require aircraftSize to create an airplane entry"));
        assertTrue(os.toString().contains("400"));
    }


}
