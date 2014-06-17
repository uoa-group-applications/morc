package nz.ac.auckland.morc.tests.orchestrated;

import nz.ac.auckland.morc.MorcTestBuilder;
import nz.ac.auckland.morc.mock.MockDefinition;
import nz.ac.auckland.morc.predicate.HeadersPredicate;
import nz.ac.auckland.morc.resource.HeadersTestResource;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple 1 expectation synchronous tests for sending and receiving messages using the Camel infrastructure
 */
public class SimpleSyncTest extends MorcTestBuilder {

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:syncInputAsyncOutput")
                        .to("seda:asyncTarget?waitForTaskToComplete=Never")
                        .setBody(constant("<foo/>"));

                //we use seda so that the from endpoint changes
                from("direct:syncInputSyncOutput")
                        .to("seda:syncTarget?waitForTaskToComplete=Always");

                from("direct:syncMultiTestPublisher")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                for (int i = 0; i < 3; i++) {
                                    template.sendBody("seda:asyncTarget", "<moo/>");
                                }
                            }
                        })
                        .setBody(constant("<foo/>"));

                from("direct:syncInputNoCallouts")
                        .setBody(constant("<abc/>"));

                from("direct:setHeaders")
                        .setHeader("foo", constant("baz"))
                        .setHeader("abc", constant("123"));

                from("direct:throwsException")
                        .throwException(new IOException());

                from("direct:asyncHandOff")
                        .to("seda:asyncExceptionThrower?waitForTaskToComplete=Never")
                        .setBody(constant("working"));
                from("seda:asyncExceptionThrower")
                        .throwException(new IOException());

                from("direct:jsonResponse")
                        .setBody(constant("{\"foo\":\"baz\"}"));

                from("direct:propertiesTest")
                        .setBody(simple("properties:response"));

                from("seda:jsonRequest")
                        .to("seda:jsonExpectation");

            }
        };
    }

    @Override
    public void configure() {
        syncTest("Simple send body to async output with valid response", "direct:syncInputAsyncOutput")
                .requestBody(xml("<baz/>"))
                .addExpectation(asyncExpectation("seda:asyncTarget").expectedBody(xml("<baz/>")))
                .expectedResponseBody(xml("<foo/>"));

        syncTest("Ensure unresolved message count is zero and still valid", "direct:syncInputAsyncOutput")
                .expectedResponseBody(xml("<foo/>"))
                .requestBody(xml("<baz/>"))
                .addExpectation(unreceivedExpectation("seda:nothingToSeeHere"));

        syncTest("Multiple messages received by expectation", "direct:syncMultiTestPublisher")
                .expectedResponseBody(xml("<foo/>"))
                .requestBody(xml("<baz/>"))
                .addExpectation(asyncExpectation("seda:asyncTarget")
                        .expectedMessageCount(3)
                        .expectedBody(xml("<moo/>")));

        syncTest("Message with no expectations", "direct:syncInputNoCallouts")
                .expectedResponseBody(xml("<abc/>"))
                .requestBody(xml("<foo/>"));

        syncTest("Test total ordering response the same", "direct:syncMultiTestPublisher")
                .requestBody(xml("<baz/>"))
                .addExpectation(asyncExpectation("seda:asyncTarget")
                        .expectedMessageCount(3).ordering(MockDefinition.OrderingType.PARTIAL));

        syncTest("Test endpoint ordering response the same", "direct:syncMultiTestPublisher")
                .requestBody(xml("<baz/>"))
                .addExpectation(asyncExpectation("seda:asyncTarget")
                        .expectedMessageCount(3).ordering(MockDefinition.OrderingType.PARTIAL));

        syncTest("Test no ordering response the same", "direct:syncMultiTestPublisher")
                .requestBody(xml("<baz/>"))
                .addExpectation(asyncExpectation("seda:asyncTarget")
                        .expectedMessageCount(3).ordering(MockDefinition.OrderingType.PARTIAL).endpointNotOrdered());

        syncTest("Test headers are handled appropriately", "direct:syncInputAsyncOutput")
                .requestBody(xml("<baz/>"))
                .requestHeaders(headers(header("foo", "baz"), header("abc", "def")))
                .addExpectation(asyncExpectation("seda:asyncTarget")
                        .expectedHeaders(headers(header("abc", "def"), header("foo", "baz"))));

        syncTest("Test sync response", "direct:syncInputSyncOutput")
                .requestBody(times(3, xml("<baz/>")))
                .addExpectation(syncExpectation("seda:syncTarget")
                        .expectedBody(times(3, xml("<baz/>")))
                        .responseBody(times(3, xml("<foo/>"))).ordering(totalOrdering()))
                .sendInterval(3000)
                .expectedResponseBody(times(3, xml("<foo/>")));

        syncTest("Test Response Headers Validated", "direct:setHeaders")
                .requestBody(text("1"))
                .expectedResponseHeaders(headers(header("abc", "123"), header("foo", "baz")));

        //we don't expect this to throw an exception back due to the async nature
        syncTest("exception thrown after async call", "direct:asyncHandOff")
                .requestBody(text("1"))
                .expectedResponseBody(text("working"));

        syncTest("test json validation in response", "direct:jsonResponse")
                .requestBody(text("1"))
                .expectedResponseBody(json("{\"foo\":\"baz\"}"));

        syncTest("check properties set correctly", "direct:propertiesTest")
                .requestBody(text("1"))
                .expectedResponseBody(text("foo"));

        syncTest("Test JSON Expectation", "seda:jsonRequest")
                .requestBody(json("{\"foo\":\"baz\"}"))
                .addExpectation(syncExpectation("seda:jsonExpectation")
                        .expectedBody(json("{\"foo\":\"baz\"}")));

        MockDefinition.MockDefinitionBuilderInit expectation1 = syncExpectation("seda:jsonExpectation")
                .expectedBody(json("{\"foo\":\"baz\"}"));
        MockDefinition.MockDefinitionBuilderInit expectation2 = unreceivedExpectation("seda:nothingToSeeHere");

        syncTest("addExpectationsTest", "seda:jsonRequest")
                .requestBody(json("{\"foo\":\"baz\"}"))
                .addExpectations(expectation1, expectation2);

        Map<String, Object> headers = new HashMap<>();
        headers.put("foo", "baz");
        headers.put("abc", "123");

        Map<String, Object> headers1 = new HashMap<>();
        headers.put("baz", "foo");
        headers.put("123", "abc");

        syncTest("Test Response Headers from Map Validated", "seda:headersFromMap")
                .requestBody(text("1"))
                .requestHeaders(new HeadersTestResource(headers))
                .addExpectation(syncExpectation("seda:headersFromMap")
                        .expectedHeaders(headers)
                        .responseHeaders(new HeadersTestResource(headers1)))
                .expectedResponseHeaders(new HeadersPredicate(new HeadersTestResource(headers1)))
                .expectedResponseBody(text("1"));

        syncTest("Test Response Headers from Map Validated 2", "seda:headersFromMap")
                .requestBody(text("1"))
                .requestHeaders(new HeadersTestResource(headers))
                .addExpectation(syncExpectation("seda:headersFromMap")
                        .expectedHeaders(headers)
                        .responseHeaders(new HeadersTestResource(headers1)))
                .expectedResponseHeaders(headers1)
                .expectedResponseBody(text("1"));

        syncTest("Test response with no expectation predicates", "direct:syncInputSyncOutput")
                .requestBody(times(3, xml("<baz/>")))
                .addExpectation(syncExpectation("seda:syncTarget").expectedMessageCount(3)
                        .responseBody(times(3, xml("<foo/>"))))
                .sendInterval(3000)
                .expectedResponseBody(times(3, xml("<foo/>")));

        //we don't normally expect the preprocessor to be applied in this way
        syncTest("Test Mock preprocessor applied", "seda:preprocessorMock")
                .requestBody(text("1"))
                .expectedResponse(new Predicate() {
                    @Override
                    public boolean matches(Exchange exchange) {
                        return exchange.getProperty("preprocessed", Boolean.class);
                    }
                }).addExpectation(syncExpectation("seda:preprocessorMock").expectedMessageCount(1)
                .mockFeedPreprocessor(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        exchange.setProperty("preprocessed", true);
                    }
                })).addPredicates(new Predicate() {
            @Override
            public boolean matches(Exchange exchange) {
                return exchange.getProperty("preprocessed", Boolean.class);
            }
        });

        syncTest("Simple Groovy JSON Test", "direct:jsonResponse")
                .requestBody(xml(groovy("<foo>$baz</foo>", var("baz", "123"))))
                .expectedResponseBody(json(groovy("{\"foo\":\"$x\"}", var("x", "baz"))));


    }

    @Override
    public String getPropertiesLocation() {
        return "test.properties";
    }
}