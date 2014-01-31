package nz.ac.auckland.integration.tests.expectation;

import nz.ac.auckland.integration.testing.expectation.UnreceivedMockDefinition;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Assert;
import org.junit.Test;

public class UnreceivedMockDefinitionTest extends Assert {

    @Test
    public void testAlwaysInvalid() throws Exception {
        UnreceivedMockDefinition expectation = new UnreceivedMockDefinition.Builder("vm:test").build();

        assertFalse(expectation.checkValid(null, -1));
        assertFalse(expectation.checkValid(new DefaultExchange(new DefaultCamelContext()), 1));
    }

    @Test
    public void testExceptionOnHandle() throws Exception {
        UnreceivedMockDefinition expectation = new UnreceivedMockDefinition.Builder("vm:test").build();

        try {
            expectation.handleReceivedExchange(null);
            assertTrue(false);
        } catch (Exception e) {
            assertEquals(e.getClass(), IllegalStateException.class);
        }
    }
}
