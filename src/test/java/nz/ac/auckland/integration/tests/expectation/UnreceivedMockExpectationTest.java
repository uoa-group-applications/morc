package nz.ac.auckland.integration.tests.expectation;

import nz.ac.auckland.integration.testing.expectation.UnreceivedMockExpectation;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Assert;
import org.junit.Test;

public class UnreceivedMockExpectationTest extends Assert {

    @Test
    public void testAlwaysInvalid() throws Exception {
        UnreceivedMockExpectation expectation = new UnreceivedMockExpectation.Builder("vm:test").build();

        assertFalse(expectation.checkValid(null, -1));
        assertFalse(expectation.checkValid(new DefaultExchange(new DefaultCamelContext()), 1));
    }

    @Test
    public void testExceptionOnHandle() throws Exception {
        UnreceivedMockExpectation expectation = new UnreceivedMockExpectation.Builder("vm:test").build();

        try {
            expectation.handleReceivedExchange(null);
            assertTrue(false);
        } catch (Exception e) {
            assertEquals(e.getClass(), IllegalStateException.class);
        }
    }
}
