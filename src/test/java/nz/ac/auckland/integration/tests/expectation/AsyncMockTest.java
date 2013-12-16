package nz.ac.auckland.integration.tests.expectation;

import nz.ac.auckland.integration.testing.expectation.AsyncMockExpectation;
import nz.ac.auckland.integration.testing.expectation.MockExpectation;
import nz.ac.auckland.integration.testing.resource.HeadersTestResource;
import nz.ac.auckland.integration.testing.resource.XmlTestResource;
import nz.ac.auckland.integration.testing.validator.HeadersValidator;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.component.seda.SedaComponent;
import org.apache.camel.component.seda.SedaEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;

public class AsyncMockTest extends Assert {

    URL bodyUrl = this.getClass().getResource("/data/xml-test1.xml");
    URL headersUrl = this.getClass().getResource("/data/header-test1.properties");

    @Test
    public void testConfiguration() throws Exception {
        AsyncMockExpectation expectation = new AsyncMockExpectation.Builder("test").build();

        assertEquals(expectation.getType(), "async");
        assertTrue(expectation.getOrderingType() != MockExpectation.OrderingType.TOTAL);
    }

    @Test
    public void testHandleReceivedExchangeUnchanged() throws Exception {
        CamelContext ctx = new DefaultCamelContext();
        Exchange exchange = new DefaultExchange(ctx);

        generateExpectationBuilder().build().handleReceivedExchange(exchange);

        assertNull(exchange.getIn().getBody());
        assertEquals(0, exchange.getIn().getHeaders().size());
        assertNull(exchange.getOut().getBody());
        assertEquals(0, exchange.getOut().getHeaders().size());
        assertEquals(0, exchange.getProperties().size());
    }

    private AsyncMockExpectation.Builder generateExpectationBuilder() {
        return new AsyncMockExpectation.Builder("seda:test")
                .name("firstExpectation")
                .expectedBody(new XmlTestResource(bodyUrl))
                .expectedHeaders(new HeadersTestResource(headersUrl));
    }

    @Test
    public void testBodyAndHeadersValid() throws Exception {

        CamelContext ctx = new DefaultCamelContext();

        Exchange exchange = new DefaultExchange(ctx);
        exchange.setFromEndpoint(new SedaEndpoint("seda://test", new SedaComponent(), null));
        exchange.getIn().setBody(new XmlTestResource(bodyUrl).getValue());
        exchange.getIn().setHeaders(new HeadersTestResource(headersUrl).getValue());

        assertTrue(generateExpectationBuilder().build().checkValid(exchange, 0));
    }

    @Test
    public void testEmptyStringBody() throws Exception {
        CamelContext ctx = new DefaultCamelContext();

        Exchange exchange = new DefaultExchange(ctx);
        exchange.setFromEndpoint(new SedaEndpoint("seda://test", new SedaComponent(), null));
        exchange.getIn().setBody("");

        assertFalse(generateExpectationBuilder().build().checkValid(exchange, 0));
    }

    @Test
    public void testNoExpectedBodyOrHeader() throws Exception {
        //for cases where we are just interested in receiving a message, and not the contents of it
        CamelContext ctx = new DefaultCamelContext();
        Exchange exchange = new DefaultExchange(ctx);
        exchange.setFromEndpoint(new SedaEndpoint("seda://test", new SedaComponent(), null));
        HeadersValidator nullHeadersValidator = null;
        XmlValidator nullValidator = null;

        assertTrue(generateExpectationBuilder().expectedHeaders(nullHeadersValidator)
                .expectedBody(nullValidator).build().checkValid(exchange, 0));
    }

    @Test
    public void testBodyNoHeaders() throws Exception {
        CamelContext ctx = new DefaultCamelContext();

        Exchange exchange = new DefaultExchange(ctx);
        exchange.getIn().setBody(new XmlTestResource(bodyUrl).getValue());
        exchange.setFromEndpoint(new SedaEndpoint("seda://test", new SedaComponent(), null));

        AsyncMockExpectation expectation = new AsyncMockExpectation.Builder("seda:test")
                .name("firstExpectation")
                .expectedBody(new XmlTestResource(bodyUrl))
                .build();

        assertTrue(expectation.checkValid(exchange, 0));
    }

    @Test
    public void testInvalidBody() throws Exception {
        CamelContext ctx = new DefaultCamelContext();

        Exchange exchange = new DefaultExchange(ctx);
        exchange.setFromEndpoint(new SedaEndpoint("seda://test", new SedaComponent(), null));
        exchange.getIn().setBody("<test/>");

        assertFalse(generateExpectationBuilder().build().checkValid(exchange, 0));
    }

    @Test
    public void testEmptyExchange() throws Exception {
        CamelContext ctx = new DefaultCamelContext();
        Exchange exchange = new DefaultExchange(ctx);
        exchange.setFromEndpoint(new SedaEndpoint("seda://test", new SedaComponent(), null));
        assertFalse(generateExpectationBuilder().build().checkValid(exchange, 0));
    }

    @Test
    public void testInvalidHeaders() throws Exception {
        CamelContext ctx = new DefaultCamelContext();

        Exchange exchange = new DefaultExchange(ctx);
        exchange.setFromEndpoint(new SedaEndpoint("seda://test", new SedaComponent(), null));
        exchange.getIn().setHeader("nothing", "here");

        assertFalse(generateExpectationBuilder().build().checkValid(exchange, 0));
    }

    @Test
    public void testOldExpectedIndex() throws Exception {
        CamelContext ctx = new DefaultCamelContext();

        Exchange exchange = new DefaultExchange(ctx);
        exchange.getIn().setBody(new XmlTestResource(bodyUrl).getValue());
        exchange.setFromEndpoint(new SedaEndpoint("seda://test", new SedaComponent(), null));
        exchange.getIn().setHeaders(new HeadersTestResource(headersUrl).getValue());

        MockExpectation expectation = generateExpectationBuilder().receivedAt(0).build();

        assertTrue(expectation.checkValid(exchange, 5));
    }

    @Test
    public void testFutureExpectedIndex() throws Exception {
        CamelContext ctx = new DefaultCamelContext();

        Exchange exchange = new DefaultExchange(ctx);
        exchange.getIn().setBody(new XmlTestResource(bodyUrl).getValue());
        exchange.setFromEndpoint(new SedaEndpoint("seda://test", new SedaComponent(), null));
        exchange.getIn().setHeaders(new HeadersTestResource(headersUrl).getValue());

        MockExpectation expectation = generateExpectationBuilder().receivedAt(1).build();

        assertFalse(expectation.checkValid(exchange, 0));
    }

    @Test
    public void testMultipleBuilders() throws Exception {

        MockExpectation expectation1 = generateExpectationBuilder().receivedAt(1).build();
        MockExpectation expectation2 = generateExpectationBuilder().receivedAt(2).build();

        assertEquals(1, expectation1.getReceivedAt());
        assertEquals(2, expectation2.getReceivedAt());

    }

}
