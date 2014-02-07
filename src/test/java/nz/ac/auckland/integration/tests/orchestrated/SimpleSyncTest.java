package nz.ac.auckland.integration.tests.orchestrated;

import nz.ac.auckland.integration.testing.MorcTestBuilder;
import nz.ac.auckland.integration.testing.mock.MockDefinition;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;

import java.io.IOException;

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
                .requestBody(xml("<baz/>"))
                .addExpectation(syncExpectation("seda:syncTarget")
                        .expectedBody(xml("<baz/>"))
                        .responseBody(xml("<foo/>")))
                .expectedResponseBody(xml("<foo/>"));

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

    }

    @Override
    public String getPropertiesLocation() {
        return "test.properties";
    }
}