package nz.ac.auckland.integration.tests.expectation;

import nz.ac.auckland.integration.testing.expectation.SyncMockExpectation;
import nz.ac.auckland.integration.testing.resource.HeadersTestResource;
import nz.ac.auckland.integration.testing.resource.XmlTestResource;
import nz.ac.auckland.integration.testing.validator.HeadersValidator;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.component.direct.DirectEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;

import java.net.URL;

public class SyncMockExpectationTest extends Assert {

    URL headersUrl = this.getClass().getResource("/data/header-test1.properties");
    URL responseUrl = this.getClass().getResource("/data/xml-response1.xml");

    @Test
    public void testBodyAndHeaders() throws Exception {
        CamelContext ctx = new DefaultCamelContext();
        Exchange exchange = new DefaultExchange(ctx);

        XmlTestResource output = new XmlTestResource(responseUrl);
        HeadersTestResource headers = new HeadersTestResource(headersUrl);
        SyncMockExpectation mockTest = new SyncMockExpectation.Builder("vm:test")
                .responseBody(output)
                .responseHeaders(headers)
                .build();

        mockTest.handleReceivedExchange(exchange);

        DetailedDiff difference = new DetailedDiff(new Diff(output.getValue(), exchange.getOut().getBody(Document.class)));
        assertTrue(difference.similar());
        assertTrue(new HeadersValidator(headers).validate(exchange.getOut().getHeaders()));
    }

    @Test
    public void testBodyNoHeaders() throws Exception {
        CamelContext ctx = new DefaultCamelContext();
        Exchange exchange = new DefaultExchange(ctx);

        XmlTestResource output = new XmlTestResource(responseUrl);
        SyncMockExpectation mockTest = new SyncMockExpectation.Builder("vm:test")
                .responseBody(output)
                .build();

        mockTest.handleReceivedExchange(exchange);

        DetailedDiff difference = new DetailedDiff(new Diff(output.getValue(), exchange.getOut().getBody(Document.class)));
        assertTrue(difference.similar());
        assertEquals(0, exchange.getOut().getHeaders().size());
    }

    @Test
    public void testNoBodyHeaders() throws Exception {
        CamelContext ctx = new DefaultCamelContext();
        Exchange exchange = new DefaultExchange(ctx);

        HeadersTestResource headers = new HeadersTestResource(headersUrl);
        SyncMockExpectation mockTest = new SyncMockExpectation.Builder("vm:test")
                .responseHeaders(headers)
                .build();

        mockTest.handleReceivedExchange(exchange);

        assertEquals("", exchange.getOut().getBody());
        assertTrue(new HeadersValidator(headers).validate(exchange.getOut().getHeaders()));
    }

    @Test
    public void testNoBodyNoHeaders() throws Exception {
        CamelContext ctx = new DefaultCamelContext();
        Exchange exchange = new DefaultExchange(ctx);

        SyncMockExpectation mockTest = new SyncMockExpectation.Builder("vm:test")
                .build();

        mockTest.handleReceivedExchange(exchange);

        assertEquals("", exchange.getOut().getBody());
        assertEquals(0, exchange.getOut().getHeaders().size());
    }

    @Test
    public void testExchangeEndpointDifferent() throws Exception {
        CamelContext ctx = new DefaultCamelContext();
        Exchange exchange = new DefaultExchange(ctx);
        exchange.setFromEndpoint(new DirectEndpoint("vm:nope", null));

        SyncMockExpectation mockTest = new SyncMockExpectation.Builder("vm:test")
                .build();

        assertFalse(mockTest.checkValid(exchange, 0));
    }

}
