package nz.ac.auckland.integration.tests.specification;

import nz.ac.auckland.integration.testing.resource.HeadersTestResource;
import nz.ac.auckland.integration.testing.resource.JsonTestResource;
import nz.ac.auckland.integration.testing.resource.PlainTextTestResource;
import nz.ac.auckland.integration.testing.resource.XmlTestResource;
import nz.ac.auckland.integration.testing.specification.SyncOrchestratedTestSpecification;
import nz.ac.auckland.integration.testing.validator.HeadersValidator;
import nz.ac.auckland.integration.testing.validator.Validator;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

import java.net.URL;

public class SyncOrchestratedTestSpecificationTest extends CamelTestSupport {
    URL bodyUrl = this.getClass().getResource("/data/xml-test1.xml");
    URL headersUrl = this.getClass().getResource("/data/header-test1.properties");
    URL responseUrl = this.getClass().getResource("/data/xml-response1.xml");

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:syncTestInput")
                        .to("mock:syncTest");
            }
        };
    }

    @Test
    public void testSendBodyAndHeadersValid() throws Exception {

        XmlTestResource input = new XmlTestResource(bodyUrl);
        HeadersTestResource headers = new HeadersTestResource(headersUrl);
        final XmlTestResource expectedOutput = new XmlTestResource(responseUrl);

        SyncOrchestratedTestSpecification spec = new SyncOrchestratedTestSpecification
                .Builder("direct:syncTestInput", "description")
                .requestBody(input)
                .requestHeaders(headers)
                .expectedResponseBody(expectedOutput)
                .build();

        MockEndpoint endpoint = getMockEndpoint("mock:syncTest");
        endpoint.expectedMessageCount(1);
        endpoint.whenAnyExchangeReceived(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                exchange.getOut().setBody(expectedOutput.getValue());
            }
        });

        assertTrue(spec.sendInput(context.createProducerTemplate()));

        endpoint.assertIsSatisfied();

        Exchange exchange = endpoint.getExchanges().get(0);

        assertTrue(input.validate(exchange));
        assertTrue(new HeadersValidator(headers).validate(exchange));

    }

    @Test
    public void testSendBodyAndHeadersInvalid() throws Exception {
        XmlTestResource input = new XmlTestResource(bodyUrl);
        HeadersTestResource headers = new HeadersTestResource(headersUrl);
        final XmlTestResource expectedOutput = new XmlTestResource(responseUrl);

        SyncOrchestratedTestSpecification spec = new SyncOrchestratedTestSpecification
                .Builder("direct:syncTestInput", "description")
                .requestBody(input)
                .requestHeaders(headers)
                .expectedResponseBody(expectedOutput)
                .build();

        MockEndpoint endpoint = getMockEndpoint("mock:syncTest");
        endpoint.expectedMessageCount(1);
        endpoint.whenAnyExchangeReceived(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                exchange.getOut().setBody("<incorrectResult/>");
            }
        });

        assertFalse(spec.sendInput(context.createProducerTemplate()));

        endpoint.assertIsSatisfied();
    }

    @Test
    public void testSendBodyNoHeaders() throws Exception {
        XmlTestResource input = new XmlTestResource(bodyUrl);
        final XmlTestResource expectedOutput = new XmlTestResource(responseUrl);

        SyncOrchestratedTestSpecification spec = new SyncOrchestratedTestSpecification
                .Builder("direct:syncTestInput", "description")
                .requestBody(input)
                .expectedResponseBody(expectedOutput)
                .build();

        MockEndpoint endpoint = getMockEndpoint("mock:syncTest");
        endpoint.expectedMessageCount(1);
        endpoint.whenAnyExchangeReceived(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                exchange.getOut().setBody(expectedOutput.getValue());
            }
        });

        assertTrue(spec.sendInput(context.createProducerTemplate()));

        endpoint.assertIsSatisfied();

        Exchange exchange = endpoint.getExchanges().get(0);

        assertTrue(input.validate(exchange));
    }

    @Test
    public void testSendNoBodyHeaders() throws Exception {
        HeadersTestResource headers = new HeadersTestResource(headersUrl);
        final XmlTestResource expectedOutput = new XmlTestResource(responseUrl);

        SyncOrchestratedTestSpecification spec = new SyncOrchestratedTestSpecification
                .Builder("direct:syncTestInput", "description")
                .requestHeaders(headers)
                .expectedResponseBody(expectedOutput)
                .build();

        MockEndpoint endpoint = getMockEndpoint("mock:syncTest");
        endpoint.expectedMessageCount(1);
        endpoint.whenAnyExchangeReceived(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                exchange.getOut().setBody(expectedOutput.getValue());
            }
        });

        assertTrue(spec.sendInput(context.createProducerTemplate()));

        endpoint.assertIsSatisfied();

        Exchange exchange = endpoint.getExchanges().get(0);

        assertEquals("", exchange.getIn().getBody(String.class));
        assertTrue(new HeadersValidator(headers).validate(exchange));
    }

    @Test
    public void testSendNoBodyNoHeaders() throws Exception {
        final XmlTestResource expectedOutput = new XmlTestResource(responseUrl);

        SyncOrchestratedTestSpecification spec = new SyncOrchestratedTestSpecification
                .Builder("direct:syncTestInput", "description")
                .expectedResponseBody(expectedOutput)
                .build();

        MockEndpoint endpoint = getMockEndpoint("mock:syncTest");
        endpoint.expectedMessageCount(1);
        endpoint.whenAnyExchangeReceived(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                exchange.getOut().setBody(expectedOutput.getValue());
            }
        });

        assertTrue(spec.sendInput(context.createProducerTemplate()));

        endpoint.assertIsSatisfied();

        Exchange exchange = endpoint.getExchanges().get(0);

        assertEquals("", exchange.getIn().getBody(String.class));
    }

    @Test
    public void testNoResponseInvalid() throws Exception {
        XmlTestResource input = new XmlTestResource(bodyUrl);
        HeadersTestResource headers = new HeadersTestResource(headersUrl);
        final XmlTestResource expectedOutput = new XmlTestResource(responseUrl);

        SyncOrchestratedTestSpecification spec = new SyncOrchestratedTestSpecification
                .Builder("direct:syncTestInput", "description")
                .requestBody(input)
                .requestHeaders(headers)
                .expectedResponseBody(expectedOutput)
                .build();

        MockEndpoint endpoint = getMockEndpoint("mock:syncTest");
        endpoint.expectedMessageCount(1);
        endpoint.whenAnyExchangeReceived(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                exchange.getOut().setBody(null);
            }
        });

        assertFalse(spec.sendInput(context.createProducerTemplate()));
    }

    @Test
    public void testNoResponseValid() throws Exception {
        //this means that no response was specified

        XmlTestResource input = new XmlTestResource(bodyUrl);
        HeadersTestResource headers = new HeadersTestResource(headersUrl);

        SyncOrchestratedTestSpecification spec = new SyncOrchestratedTestSpecification
                .Builder("direct:syncTestInput", "description")
                .requestBody(input)
                .requestHeaders(headers)
                .build();

        MockEndpoint endpoint = getMockEndpoint("mock:syncTest");
        endpoint.expectedMessageCount(1);
        endpoint.whenAnyExchangeReceived(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                exchange.getOut().setBody(null);
            }
        });

        assertTrue(spec.sendInput(context.createProducerTemplate()));
    }

    @Test
    public void testSendBodyWithNoExpectedResponse() throws Exception {

        XmlTestResource input = new XmlTestResource(bodyUrl);
        HeadersTestResource headers = new HeadersTestResource(headersUrl);

        SyncOrchestratedTestSpecification spec = new SyncOrchestratedTestSpecification
                .Builder("direct:syncTestInput", "description")
                .requestBody(input)
                .requestHeaders(headers)
                .build();

        MockEndpoint endpoint = getMockEndpoint("mock:syncTest");
        endpoint.expectedMessageCount(1);

        assertTrue(spec.sendInput(context.createProducerTemplate()));

        endpoint.assertIsSatisfied();

        Exchange exchange = endpoint.getExchanges().get(0);

        assertTrue(input.validate(exchange));
        assertTrue(new HeadersValidator(headers).validate(exchange));

    }

    @Test
    public void testSetExpectedResponseBodyJson() throws Exception {
        final JsonTestResource input = new JsonTestResource(this.getClass().getResource("/data/json-test1.json"));

        SyncOrchestratedTestSpecification spec = new SyncOrchestratedTestSpecification
                .Builder("direct:syncTestInput", "description")
                .requestBody(input)
                .expectedResponseBody(input)
                .build();

        MockEndpoint endpoint = getMockEndpoint("mock:syncTest");
        endpoint.expectedMessageCount(1);
        endpoint.whenAnyExchangeReceived(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                exchange.getOut().setBody(input.getValue());
            }
        });

        assertTrue(spec.sendInput(context.createProducerTemplate()));

        endpoint.assertIsSatisfied();

    }

    @Test
    public void testBuildExpectsExceptionResponseBodySet() throws Exception {
        IllegalArgumentException e = null;
        try {
            SyncOrchestratedTestSpecification spec = new SyncOrchestratedTestSpecification
                    .Builder("direct:syncTestInput", "description")
                    .expectsExceptionResponse()
                    .expectedResponseBody(new PlainTextTestResource("foo"))
                    .build();
        } catch (IllegalArgumentException ex) {
            e = ex;
        }
        assertNotNull(e);
    }

    @Test
    public void testBuildExceptionValidatorResponseBodySet() throws Exception {
        IllegalArgumentException e = null;
        try {
            SyncOrchestratedTestSpecification spec = new SyncOrchestratedTestSpecification
                    .Builder("direct:syncTestInput", "description")
                    .expectedResponse(new Validator() {
                        @Override
                        public boolean validate(Exchange exchange) {
                            return false;
                        }
                    })
                    .expectsExceptionResponse()
                    .expectedResponseBody(new PlainTextTestResource("foo"))
                    .build();
        } catch (IllegalArgumentException ex) {
            e = ex;
        }
        assertNotNull(e);
    }

    @Test
    public void testBuildExpectsExceptionResponseHeadersSet() throws Exception {
        HeadersTestResource headers = new HeadersTestResource(headersUrl);
        IllegalArgumentException e = null;
        try {
            SyncOrchestratedTestSpecification spec = new SyncOrchestratedTestSpecification
                    .Builder("direct:syncTestInput", "description")
                    .expectsExceptionResponse()
                    .expectedResponseHeaders(headers)
                    .build();
        } catch (IllegalArgumentException ex) {
            e = ex;
        }
        assertNotNull(e);
    }

    @Test
    public void testBuildExpectsExceptionResponseHeaderAndBodySet() throws Exception {
        HeadersTestResource headers = new HeadersTestResource(headersUrl);
        IllegalArgumentException e = null;
        try {
            SyncOrchestratedTestSpecification spec = new SyncOrchestratedTestSpecification
                    .Builder("direct:syncTestInput", "description")
                    .expectedResponseBody(new PlainTextTestResource("foo"))
                    .expectsExceptionResponse()
                    .expectedResponseHeaders(headers)
                    .build();
        } catch (IllegalArgumentException ex) {
            e = ex;
        }
        assertNotNull(e);
    }

}
