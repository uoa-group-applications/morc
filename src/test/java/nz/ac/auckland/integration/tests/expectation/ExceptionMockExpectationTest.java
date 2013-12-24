package nz.ac.auckland.integration.tests.expectation;

import nz.ac.auckland.integration.testing.answer.Answer;
import nz.ac.auckland.integration.testing.expectation.ExceptionMockExpectation;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class ExceptionMockExpectationTest extends Assert {
    @Test
    public void testExceptionAndMessage() throws Exception {
        ExceptionMockExpectation expectation = new ExceptionMockExpectation.Builder("vm:test")
                .exceptionResponse(new Answer<Exception>() {
                    @Override
                    public Exception response(Exchange exchange) throws Exception {
                        return new IOException("test");
                    }
                })
                .build();

        try {
            expectation.handleReceivedExchange(new DefaultExchange(new DefaultCamelContext()));
        } catch (IOException e) {
            assert (e.getMessage().equals("test"));
        }
    }

    @Test
    public void testExceptionNoMessage() throws Exception {
        ExceptionMockExpectation expectation = new ExceptionMockExpectation.Builder("vm:test")
                .exceptionResponse(new Answer<Exception>() {
                    @Override
                    public Exception response(Exchange exchange) throws Exception {
                        return new IOException();
                    }
                })
                .build();

        try {
            expectation.handleReceivedExchange(new DefaultExchange(new DefaultCamelContext()));
        } catch (IOException e) {
            assert (e.getMessage() == null || e.getMessage().equals(""));
        }
    }

    @Test
    public void testNoException() throws Exception {
        ExceptionMockExpectation expectation = new ExceptionMockExpectation.Builder("vm:test")
                .build();

        try {
            expectation.handleReceivedExchange(new DefaultExchange(new DefaultCamelContext()));
        } catch (Exception e) {
            assert (e.getMessage() == null || e.getMessage().equals(""));
        }
    }
}
