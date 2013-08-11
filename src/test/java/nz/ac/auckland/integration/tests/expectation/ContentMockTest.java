package nz.ac.auckland.integration.tests.expectation;

import nz.ac.auckland.integration.testing.expectation.ContentMockExpectation;
import nz.ac.auckland.integration.testing.resource.HeadersTestResource;
import nz.ac.auckland.integration.testing.resource.XmlTestResource;
import org.apache.camel.Exchange;
import org.apache.camel.component.seda.SedaEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ContentMockTest extends Assert {

    URL bodyUrl = this.getClass().getResource("/data/xml-test1.xml");
    URL headersUrl = this.getClass().getResource("/data/header-test1.properties");

    @Test
    public void testNoBodyNoHeaders() throws Exception {
        TestContentMockExpectation expectation = new TestContentMockExpectation.Builder("seda:test").build();
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.setFromEndpoint(new SedaEndpoint("seda://test", null, null));
        assertTrue(expectation.checkValid(exchange, 0));
    }

    @Test
    public void testBodyNoHeaders() throws Exception {
        XmlTestResource resource = new XmlTestResource(bodyUrl);
        TestContentMockExpectation expectation = new TestContentMockExpectation.Builder("seda:test")
                .expectedBody(resource).build();

        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.setFromEndpoint(new SedaEndpoint("seda://test", null, null));
        exchange.getIn().setBody(resource.getValue());

        assertTrue(expectation.checkValid(exchange, 0));
    }

    @Test
    public void testBodyAndHeaders() throws Exception {
        XmlTestResource resource = new XmlTestResource(bodyUrl);
        HeadersTestResource headers = new HeadersTestResource(headersUrl);
        TestContentMockExpectation expectation = new TestContentMockExpectation.Builder("seda:test")
                .expectedBody(resource)
                .expectedHeaders(headers)
                .build();

        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.setFromEndpoint(new SedaEndpoint("seda://test", null, null));
        exchange.getIn().setBody(resource.getValue());
        exchange.getIn().setHeaders(headers.getValue());

        assertTrue(expectation.checkValid(exchange, 0));
    }

    @Test
    public void testInvalidBody() throws Exception {
        XmlTestResource resource = new XmlTestResource(bodyUrl);
        TestContentMockExpectation expectation = new TestContentMockExpectation.Builder("seda:test")
                .expectedBody(resource).build();

        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.setFromEndpoint(new SedaEndpoint("seda://test", null, null));
        exchange.getIn().setBody("<somebody/>");

        assertFalse(expectation.checkValid(exchange, 0));
    }

    @Test
    public void testInvalidHeaders() throws Exception {
        XmlTestResource resource = new XmlTestResource(bodyUrl);
        HeadersTestResource headers = new HeadersTestResource(headersUrl);

        TestContentMockExpectation expectation = new TestContentMockExpectation.Builder("seda:test")
                .expectedBody(resource)
                .expectedHeaders(headers)
                .build();

        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.setFromEndpoint(new SedaEndpoint("seda://test", null, null));
        exchange.getIn().setBody(resource.getValue());

        Map<String, Object> wrongHeaders = new HashMap<>();
        wrongHeaders.put("wrong", "wrong");

        exchange.getIn().setHeaders(wrongHeaders);

        assertFalse(expectation.checkValid(exchange, 0));
    }

}

class TestContentMockExpectation extends ContentMockExpectation {

    @Override
    public void handleReceivedExchange(Exchange exchange) throws Exception {

    }

    @Override
    public String getType() {
        return "test";
    }

    public static class Builder extends ContentMockExpectation.AbstractContentBuilder<TestContentMockExpectation, Builder> {

        public Builder(String endpointUri) {
            super(endpointUri);
        }

        protected Builder self() {
            return this;
        }

        public TestContentMockExpectation build() {
            return new TestContentMockExpectation(this);
        }
    }

    protected TestContentMockExpectation(Builder builder) {
        super(builder);
    }
}


