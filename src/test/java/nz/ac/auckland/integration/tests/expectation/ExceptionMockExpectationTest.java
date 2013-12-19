package nz.ac.auckland.integration.tests.expectation;

import nz.ac.auckland.integration.testing.expectation.ExceptionMockExpectation;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Assert;
import org.junit.Test;

import java.awt.*;
import java.io.IOException;
import java.security.acl.AclNotFoundException;

public class ExceptionMockExpectationTest extends Assert {
    @Test
    public void testExceptionAndMessage() throws Exception {
        ExceptionMockExpectation expectation = new ExceptionMockExpectation.Builder("vm:test")
                .exceptionResponse(new )
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
                .exceptionClass(IOException.class)
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

    @Test
    public void testNoExceptionMessage() throws Exception {
        ExceptionMockExpectation expectation = new ExceptionMockExpectation.Builder("vm:test")
                .message("test")
                .build();

        try {
            expectation.handleReceivedExchange(new DefaultExchange(new DefaultCamelContext()));
        } catch (Exception e) {
            assert (e.getMessage().equals("test"));
        }
    }

    @Test
    public void testExceptionWithMessageNoSuchConstructor() throws Exception {
        //an example of an exception with no string message constructor
        try {
            ExceptionMockExpectation expectation = new ExceptionMockExpectation.Builder("vm:test")
                    .exceptionClass(AclNotFoundException.class)
                    .message("test")
                    .build();
            assertFalse(true);
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof NoSuchMethodException);
        }
    }

    @Test
    public void testExceptionNoMessageNoDefaultConstructor() throws Exception {
        //an example of an exception with no default constructor
        try {
            ExceptionMockExpectation expectation = new ExceptionMockExpectation.Builder("vm:test")
                    .exceptionClass(FontFormatException.class)
                    .build();
            assertFalse(true);
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof NoSuchMethodException);
        }
    }

}
