package nz.ac.auckland.integration.tests.orchestrated;

import nz.ac.auckland.integration.testing.OrchestratedTestBuilder;
import nz.ac.auckland.integration.testing.expectation.MockExpectation;
import nz.ac.auckland.integration.testing.validator.HeadersValidator;
import nz.ac.auckland.integration.testing.validator.Validator;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple 1 expectation synchronous tests for sending and receiving messages using the Camel infrastructure
 */
public class SimpleSyncTest extends OrchestratedTestBuilder {

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
                        .to("seda:syncTarget");

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
                        .setHeader("foo",constant("baz"))
                        .setHeader("abc",constant("123"));

                from("direct:throwsException")
                        .throwException(new IOException());

            }
        };
    }

    public static void configure() {
        syncTest("direct:syncInputAsyncOutput", "Simple send body to async output with valid response")
                .requestBody(xml("<baz/>"))
                .addExpectation(asyncExpectation("seda:asyncTarget").expectedBody(xml("<baz/>")))
                .expectedResponseBody(xml("<foo/>"));

        syncTest("direct:syncInputAsyncOutput", "Ensure unresolved message count is zero and still valid")
                .expectedResponseBody(xml("<foo/>"))
                .requestBody(xml("<baz/>"))
                .addExpectation(unreceivedExpectation("seda:nothingToSeeHere"));

        syncTest("direct:syncMultiTestPublisher", "Multiple messages received by expectation")
                .expectedResponseBody(xml("<foo/>"))
                .requestBody(xml("<baz/>"))
                .addExpectation(asyncExpectation("seda:asyncTarget")
                        .expectedMessageCount(3)
                        .expectedBody(xml("<moo/>")));

        syncTest("direct:syncInputNoCallouts", "Message with no expectations")
                .expectedResponseBody(xml("<abc/>"))
                .requestBody(xml("<foo/>"));

        syncTest("direct:syncMultiTestPublisher", "Test total ordering response the same")
                .requestBody(xml("<baz/>"))
                .addExpectation(asyncExpectation("seda:asyncTarget")
                        .expectedMessageCount(3).ordering(MockExpectation.OrderingType.PARTIAL));

        syncTest("direct:syncMultiTestPublisher", "Test endpoint ordering response the same")
                .requestBody(xml("<baz/>"))
                .addExpectation(asyncExpectation("seda:asyncTarget")
                        .expectedMessageCount(3).ordering(MockExpectation.OrderingType.PARTIAL));

        syncTest("direct:syncMultiTestPublisher", "Test no ordering response the same")
                .requestBody(xml("<baz/>"))
                .addExpectation(asyncExpectation("seda:asyncTarget")
                        .expectedMessageCount(3).ordering(MockExpectation.OrderingType.PARTIAL).endpointNotOrdered());

        syncTest("direct:syncInputAsyncOutput", "Test headers are handled appropriately")
                .requestBody(xml("<baz/>"))
                .requestHeaders(headers(header("foo", "baz"), header("abc", "def")))
                .addExpectation(asyncExpectation("seda:asyncTarget")
                        .expectedHeaders(headers(header("abc", "def"), header("foo", "baz"))));

        syncTest("direct:syncInputSyncOutput", "Test sync response")
                .requestBody(xml("<baz/>"))
                .addExpectation(syncExpectation("seda:syncTarget")
                        .expectedBody(xml("<baz/>")).responseBody(xml("<foo/>")))
                .expectedResponseBody(xml("<foo/>"));

        syncTest("direct:setHeaders","Test Response Headers Validated")
                .expectedResponseHeaders(headers(header("abc","123"),header("foo","baz")));

        syncTest("direct:throwsException","exception found, expectsExceptionResponse and no validator")
                .expectsExceptionResponse();

        syncTest("direct:throwsException","exception found and exception validator = true")
                .expectsExceptionResponse()
                .exceptionResponseValidator(new Validator() {
                    @Override
                    public boolean validate(Exchange exchange) {
                        return exchange.getException() instanceof IOException;
                    }
                });




    }
}