package nz.ac.auckland.integration.tests.expectation;

import nz.ac.auckland.integration.testing.expectation.MockExpectation;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.component.seda.SedaComponent;
import org.apache.camel.component.seda.SedaEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Assert;
import org.junit.Test;

public class MessageOrderingMockTest extends Assert {

    @Test
    public void testTotalOrderedEndpointTotalOrderedOneMessage() throws Exception {
        TestMockExpectation expectation = new TestMockExpectation.Builder("seda:test")
                .receivedAt(1)
                .build();

        CamelContext ctx = new DefaultCamelContext();
        Exchange exchange = new DefaultExchange(ctx);
        exchange.setFromEndpoint(new SedaEndpoint("seda://test", new SedaComponent(), null));

        assertTrue(expectation.checkValid(exchange, 1));
        assertFalse(expectation.checkValid(exchange, 0));
        assertFalse(expectation.checkValid(exchange, 2));
    }

    @Test
    public void testTotalOrderedEndpointTotalOrderedManyMessages() throws Exception {

        TestMockExpectation expectation = new TestMockExpectation.Builder("seda:test")
                .receivedAt(1)
                .expectedMessageCount(3)
                .build();

        CamelContext ctx = new DefaultCamelContext();
        Exchange exchange = new DefaultExchange(ctx);
        exchange.setFromEndpoint(new SedaEndpoint("seda://test", new SedaComponent(), null));

        assertFalse(expectation.checkValid(exchange, 0));
        assertTrue(expectation.checkValid(exchange, 1));
        assertTrue(expectation.checkValid(exchange, 2));
        assertTrue(expectation.checkValid(exchange, 3));
        assertFalse(expectation.checkValid(exchange, 4));
    }

    @Test
    public void testTotalOrderedNotEndpointTotalOrderedOneMessage() throws Exception {
        TestMockExpectation expectation = new TestMockExpectation.Builder("seda:test")
                .receivedAt(1)
                .endpointNotOrdered()
                .expectedMessageCount(1)
                .build();

        CamelContext ctx = new DefaultCamelContext();
        Exchange exchange = new DefaultExchange(ctx);
        exchange.setFromEndpoint(new SedaEndpoint("seda://test", new SedaComponent(), null));

        assertFalse(expectation.checkValid(exchange, 0));
        assertTrue(expectation.checkValid(exchange, 1));
        assertFalse(expectation.checkValid(exchange, 2));
    }

    @Test
    public void testTotalOrderedNotEndpointTotalOrderedManyMessages() throws Exception {
        TestMockExpectation expectation = new TestMockExpectation.Builder("seda:test")
                .receivedAt(1)
                .endpointNotOrdered()
                .expectedMessageCount(3)
                .build();

        CamelContext ctx = new DefaultCamelContext();
        Exchange exchange = new DefaultExchange(ctx);
        exchange.setFromEndpoint(new SedaEndpoint("seda://test", new SedaComponent(), null));

        assertFalse(expectation.checkValid(exchange, 0));
        assertTrue(expectation.checkValid(exchange, 1));
        assertTrue(expectation.checkValid(exchange, 2));
        assertTrue(expectation.checkValid(exchange, 3));
        assertFalse(expectation.checkValid(exchange, 4));
    }

    @Test
    public void testNotTotalOrderedEndpointTotalOrderedOneMessage() throws Exception {
        TestMockExpectation expectation = new TestMockExpectation.Builder("seda:test")
                .receivedAt(1)
                .ordering(MockExpectation.OrderingType.PARTIAL)
                .expectedMessageCount(1)
                .build();

        CamelContext ctx = new DefaultCamelContext();
        Exchange exchange = new DefaultExchange(ctx);
        exchange.setFromEndpoint(new SedaEndpoint("seda://test", new SedaComponent(), null));

        assertFalse(expectation.checkValid(exchange, 0));
        assertTrue(expectation.checkValid(exchange, 1));
        assertTrue(expectation.checkValid(exchange, 2));
    }

    @Test
    public void testNotTotalOrderedEndpointTotalOrderedManyMessages() throws Exception {
        TestMockExpectation expectation = new TestMockExpectation.Builder("seda:test")
                .receivedAt(1)
                .ordering(MockExpectation.OrderingType.PARTIAL)
                .expectedMessageCount(3)
                .build();

        CamelContext ctx = new DefaultCamelContext();
        Exchange exchange = new DefaultExchange(ctx);
        exchange.setFromEndpoint(new SedaEndpoint("seda://test", new SedaComponent(), null));

        assertFalse(expectation.checkValid(exchange, 0));
        assertTrue(expectation.checkValid(exchange, 1));
        assertTrue(expectation.checkValid(exchange, 2));
        assertTrue(expectation.checkValid(exchange, 3));
        assertTrue(expectation.checkValid(exchange, 4));
    }

    @Test
    public void testNotTotalOrderedNotEndpointTotalOrderedOneMessage() throws Exception {
        TestMockExpectation expectation = new TestMockExpectation.Builder("seda:test")
                .receivedAt(1)
                .ordering(MockExpectation.OrderingType.PARTIAL)
                .endpointNotOrdered()
                .expectedMessageCount(1)
                .build();

        CamelContext ctx = new DefaultCamelContext();
        Exchange exchange = new DefaultExchange(ctx);
        exchange.setFromEndpoint(new SedaEndpoint("seda://test", new SedaComponent(), null));

        assertFalse(expectation.checkValid(exchange, 0));
        assertTrue(expectation.checkValid(exchange, 1));
        assertTrue(expectation.checkValid(exchange, 2));
    }

    @Test
    public void testNotTotalOrderedNotEndpointTotalOrderedManyMessages() throws Exception {
        TestMockExpectation expectation = new TestMockExpectation.Builder("seda:test")
                .receivedAt(1)
                .endpointNotOrdered()
                .ordering(MockExpectation.OrderingType.PARTIAL)
                .expectedMessageCount(3)
                .build();

        CamelContext ctx = new DefaultCamelContext();
        Exchange exchange = new DefaultExchange(ctx);
        exchange.setFromEndpoint(new SedaEndpoint("seda://test", new SedaComponent(), null));

        assertFalse(expectation.checkValid(exchange, 0));
        assertTrue(expectation.checkValid(exchange, 1));
        assertTrue(expectation.checkValid(exchange, 2));
        assertTrue(expectation.checkValid(exchange, 3));
        assertTrue(expectation.checkValid(exchange, 4));
    }

    @Test
    public void testNoneOrdering() throws Exception {
        TestMockExpectation expectation = new TestMockExpectation.Builder("seda:test")
                .receivedAt(1)
                .endpointNotOrdered()
                .ordering(MockExpectation.OrderingType.NONE)
                .expectedMessageCount(3)
                .build();

        CamelContext ctx = new DefaultCamelContext();
        Exchange exchange = new DefaultExchange(ctx);
        exchange.setFromEndpoint(new SedaEndpoint("seda://test", new SedaComponent(), null));

        assertTrue(expectation.checkValid(exchange, -1));
        assertTrue(expectation.checkValid(exchange, 0));
        assertTrue(expectation.checkValid(exchange, 99));
    }

}

//I'm interesting in seeing how the super class behaves
class TestMockExpectation extends MockExpectation {

    @Override
    public void handleReceivedExchange(Exchange exchange) throws Exception {
        //noop
    }

    public static class Builder extends MockExpectation.AbstractBuilder<TestMockExpectation, Builder> {

        public Builder(String endpointUri) {
            super(endpointUri);
        }

        protected Builder self() {
            return this;
        }

        public TestMockExpectation buildInternal() {
            return new TestMockExpectation(this);
        }

        protected int expectedMessageCount() {
            return 1;
        }
    }

    @Override
    public String getType() {
        return "test";
    }

    protected TestMockExpectation(Builder builder) {
        super(builder);
    }
}
