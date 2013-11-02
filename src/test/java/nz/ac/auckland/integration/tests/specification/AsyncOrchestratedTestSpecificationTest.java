package nz.ac.auckland.integration.tests.specification;

import nz.ac.auckland.integration.testing.expectation.UnreceivedMockExpectation;
import nz.ac.auckland.integration.testing.resource.HeadersTestResource;
import nz.ac.auckland.integration.testing.resource.XmlTestResource;
import nz.ac.auckland.integration.testing.specification.AsyncOrchestratedTestSpecification;
import nz.ac.auckland.integration.testing.validator.HeadersValidator;
import nz.ac.auckland.integration.testing.validator.XmlValidator;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

import java.net.URL;

public class AsyncOrchestratedTestSpecificationTest extends CamelTestSupport {

    URL bodyUrl = this.getClass().getResource("/data/xml-test1.xml");
    URL headersUrl = this.getClass().getResource("/data/header-test1.properties");

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("seda:asyncTestInput")
                        .to("mock:asyncTest");
            }
        };
    }

    @Test
    public void testSendBodyAndHeaders() throws Exception {

        XmlTestResource input = new XmlTestResource(bodyUrl);
        HeadersTestResource headers = new HeadersTestResource(headersUrl);

        AsyncOrchestratedTestSpecification spec = new AsyncOrchestratedTestSpecification
                .Builder("seda:asyncTestInput", "description")
                .addExpectation(new UnreceivedMockExpectation.Builder("seda:nowhere"))
                .inputHeaders(headers)
                .inputMessage(input)
                .build();

        MockEndpoint endpoint = getMockEndpoint("mock:asyncTest");
        endpoint.expectedMessageCount(1);

        assertTrue(spec.sendInput(context.createProducerTemplate()));

        endpoint.assertIsSatisfied();

        Exchange exchange = endpoint.getExchanges().get(0);

        assertTrue(new XmlValidator(input).validate(exchange));
        assertTrue(new HeadersValidator(headers).validate(exchange));
    }

    @Test
    public void testSendBodyNoHeaders() throws Exception {

        XmlTestResource input = new XmlTestResource(bodyUrl);

        AsyncOrchestratedTestSpecification spec = new AsyncOrchestratedTestSpecification
                .Builder("seda:asyncTestInput", "description")
                .inputMessage(input)
                .addExpectation(new UnreceivedMockExpectation.Builder("seda:nowhere"))
                .build();

        MockEndpoint endpoint = getMockEndpoint("mock:asyncTest");
        endpoint.expectedMessageCount(1);

        assertTrue(spec.sendInput(context.createProducerTemplate()));

        endpoint.assertIsSatisfied();

        Exchange exchange = endpoint.getExchanges().get(0);

        assertTrue(new XmlValidator(input).validate(exchange));
    }

    @Test
    public void testSendNoBodyNoHeaders() throws Exception {
        AsyncOrchestratedTestSpecification spec = new AsyncOrchestratedTestSpecification
                .Builder("seda:asyncTestInput", "description")
                .addExpectation(new UnreceivedMockExpectation.Builder("seda:nowhere"))
                .build();

        MockEndpoint endpoint = getMockEndpoint("mock:asyncTest");
        endpoint.expectedMessageCount(1);

        assertTrue(spec.sendInput(context.createProducerTemplate()));

        endpoint.assertIsSatisfied();

        Exchange exchange = endpoint.getExchanges().get(0);

        assertEquals("", exchange.getIn().getBody(String.class));
    }

    @Test
    public void testBuildNoExpectations() throws Exception {
        Exception e = null;
        try {
            AsyncOrchestratedTestSpecification spec = new AsyncOrchestratedTestSpecification
                .Builder("seda:asyncTestInput", "description")
                .build();
        } catch (Exception ex) {
            e = ex;
        }

        assertNotNull(e);
        assertIsInstanceOf(IllegalArgumentException.class,e);
    }

}
