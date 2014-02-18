package nz.ac.auckland.integration.tests.orchestrated;

import nz.ac.auckland.integration.testing.MorcTestBuilder;
import nz.ac.auckland.integration.testing.mock.MockDefinition;
import nz.ac.auckland.integration.testing.predicate.HeadersPredicate;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;

import java.io.FileNotFoundException;
import java.io.IOException;


/**
 * Simple 1 expectation synchronous tests for sending and receiving messages using the Camel infrastructure
 */
public class MultiExpectationSyncTest extends MorcTestBuilder {

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:syncInput")
                        .to("seda:asyncTargetInternal?waitForTaskToComplete=Never")
                        .to("seda:syncTarget?waitForTaskToComplete=Always");

                from("seda:asyncTargetInternal")
                        .setBody(constant("<async/>"))
                        .delay(2500)
                        .to("seda:asyncTarget");

                from("seda:asyncTargetInternal1")
                        .setBody(constant("<async/>"))
                        .delay(5000)
                        .to("seda:asyncTarget1");

                from("direct:syncInputMultiAsync")
                        .to("seda:asyncTargetInternal?waitForTaskToComplete=Never")
                        .to("seda:asyncTargetInternal1?waitForTaskToComplete=Never")
                        .to("seda:syncTarget?waitForTaskToComplete=Always");

                from("direct:syncInputMultiAsyncToSameDest")
                        .to("seda:asyncTargetInternal?waitForTaskToComplete=Never")
                        .to("seda:asyncTargetInternal1?waitForTaskToComplete=Never")
                        .to("seda:asyncTargetInternal1?waitForTaskToComplete=Never")
                        .to("seda:syncTarget?waitForTaskToComplete=Always");

                from("direct:multiSend")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                template.sendBody("seda:syncMultiSendEndpoint0", exchange.getIn().getBody());
                                Thread.sleep(100);
                                template.sendBody("seda:syncMultiSendEndpoint2?waitForTaskToComplete=Never", exchange.getIn().getBody());
                                Thread.sleep(5000);
                                template.sendBody("seda:syncMultiSendEndpoint1?waitForTaskToComplete=Never", exchange.getIn().getBody());
                            }
                        });

                from("direct:multiSend1")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                template.sendBody("seda:syncMultiSendEndpoint0", exchange.getIn().getBody());
                                Thread.sleep(100);
                                template.sendBody("seda:syncMultiSendEndpoint1", "<first/>");
                                Thread.sleep(5000);
                                template.sendBody("seda:syncMultiSendEndpoint1", "<second/>");
                            }
                        });

                from("direct:multiSend2")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                template.sendBody("seda:syncMultiSendEndpoint0", exchange.getIn().getBody());
                                Thread.sleep(100);
                                template.sendBody("seda:syncMultiSendEndpoint2", "<first/>");
                                Thread.sleep(2000);
                                template.sendBody("seda:syncMultiSendEndpoint1", "<second/>");
                                Thread.sleep(2000);
                                template.sendBody("seda:syncMultiSendEndpoint2", "<third/>");
                                Thread.sleep(2000);
                                template.sendBody("seda:syncMultiSendEndpoint1", "<fourth/>");
                            }

                        });

                from("direct:syncAtEnd")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                template.sendBody("seda:b", "");
                                Thread.sleep(2000);
                                template.sendBody("seda:a", "1");
                                Thread.sleep(2000);
                                template.sendBody("seda:a", "2");
                            }

                        });

                from("direct:endpointUnorderedWithSyncInMiddle")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                template.sendBody("seda:a", "3");
                                Thread.sleep(1000);
                                template.sendBody("seda:a", "2");
                                Thread.sleep(1000);
                                template.sendBody("seda:s", "1");
                                Thread.sleep(1000);
                                template.sendBody("seda:a", "1");
                            }

                        });

                from("direct:endpointWithSyncOrdering")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                template.sendBody("seda:b", "");
                                Thread.sleep(1000);
                                template.sendBody("seda:s", "");
                                Thread.sleep(1000);
                                template.sendBody("seda:a", "");
                            }
                        });

                from("vm:asyncSyncAndEndpointNotOrderedDontMix")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                template.sendBody("vm:a", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
                                Thread.sleep(500);
                                template.sendBody("vm:a", "2");
                                Thread.sleep(500);
                                template.sendBody("vm:s", "");
                                Thread.sleep(500);
                                template.sendBody("vm:a", "1");
                            }
                        });
            }
        };
    }

    @Override
    public void configure() {
        syncTest("Simple send body to two destinations and get correct response", "direct:syncInput")
        .expectedResponseBody(xml("<foo/>"))
                .requestBody(xml("<baz/>"))
                .addExpectation(asyncExpectation("seda:asyncTarget").expectedBody(xml("<async/>")))
                .addExpectation(syncExpectation("seda:syncTarget").expectedBody(xml("<baz/>")).
                        responseBody(xml("<foo/>")));

        syncTest("Simple send body to two destinations with swapped order", "direct:syncInput")
                .expectedResponseBody(xml("<foo/>"))
                .requestBody(xml("<baz/>"))
                .addExpectation(syncExpectation("seda:syncTarget")
                        .expectedBody(xml("<baz/>"))
                        .responseBody(xml("<foo/>")))
                .addExpectation(asyncExpectation("seda:asyncTarget").expectedBody(xml("<async/>")));

        syncTest("Sync and multiple Async - ensuring total order", "direct:syncInputMultiAsync")
                .expectedResponseBody(xml("<foo/>"))
                .requestBody(xml("<baz/>"))
                        //this expectation will come in last
                .addExpectation(asyncExpectation("seda:asyncTarget1").expectedBody(xml("<async/>")))
                        //this expectation will come in first
                .addExpectation(syncExpectation("seda:syncTarget").expectedBody(xml("<baz/>")).
                        responseBody(xml("<foo/>")))
                        //this expectation will come in second to last
                .addExpectation(asyncExpectation("seda:asyncTarget").expectedBody(xml("<async/>")));

        syncTest("Sync and multiple Async to same dest - ensuring total order", "direct:syncInputMultiAsyncToSameDest")
                .expectedResponseBody(xml("<foo/>"))
                .requestBody(xml("<baz/>"))
                .addExpectation(asyncExpectation("seda:asyncTarget1").expectedBody(xml("<async/>")))
                .addExpectation(syncExpectation("seda:syncTarget").expectedBody(xml("<baz/>")).
                        responseBody(xml("<foo/>")))
                .addExpectation(asyncExpectation("seda:asyncTarget").expectedBody(xml("<async/>")))
                .addExpectation(asyncExpectation("seda:asyncTarget1"));

        syncTest("Send to two sync destinations without total ordering", "direct:multiSend")
                .requestBody(xml("<foo/>"))
                .addExpectation(syncExpectation("seda:syncMultiSendEndpoint0").expectedMessageCount(1))
                        //this will receive the message last
                .addExpectation(syncExpectation("seda:syncMultiSendEndpoint1").expectedMessageCount(1)
                        .ordering(MockDefinition.OrderingType.PARTIAL))
                        //this will receive the message first
                .addExpectation(syncExpectation("seda:syncMultiSendEndpoint2").expectedMessageCount(1)
                        .ordering(MockDefinition.OrderingType.PARTIAL));

        syncTest("Send unordered messages to same sync endpoint without endpoint ordering", "direct:multiSend1")
                .requestBody(xml("<foo/>"))
                .addExpectation(syncExpectation("seda:syncMultiSendEndpoint0").expectedMessageCount(1))
                        //we will receive this last
                .addExpectation(syncExpectation("seda:syncMultiSendEndpoint1").endpointNotOrdered()
                        .expectedBody(xml("<second/>")))
                        //we will receive this first
                .addExpectation(syncExpectation("seda:syncMultiSendEndpoint1").endpointNotOrdered()
                        .expectedBody(xml("<first/>")));

        syncTest("Send unordered messages to two different sync destinations without total ordering or endpoint ordering", "direct:multiSend2")
                .requestBody(xml("<foo/>"))
                .addExpectation(syncExpectation("seda:syncMultiSendEndpoint0").expectedMessageCount(1))
                .addExpectation(syncExpectation("seda:syncMultiSendEndpoint1").endpointNotOrdered().ordering(partialOrdering())
                        .expectedBody(xml("<fourth/>")))
                .addExpectation(syncExpectation("seda:syncMultiSendEndpoint2").endpointNotOrdered().ordering(partialOrdering())
                        .expectedBody(xml("<third/>")))
                .addExpectation(syncExpectation("seda:syncMultiSendEndpoint1").endpointNotOrdered().ordering(partialOrdering())
                        .expectedBody(xml("<second/>")))
                .addExpectation(syncExpectation("seda:syncMultiSendEndpoint2").endpointNotOrdered().ordering(partialOrdering())
                        .expectedBody(xml("<first/>")));

        syncTest("Send async messages out of order such that sync arrives first", "direct:syncAtEnd")
                .requestBody(text("0"))
                .addExpectation(asyncExpectation("seda:a").expectedBody(text("2")).endpointNotOrdered().expectedMessageCount(1))
                .addExpectation(asyncExpectation("seda:a").expectedBody(text("1")).endpointNotOrdered().expectedMessageCount(1))
                .addExpectation(syncExpectation(("seda:b")).expectedMessageCount(1));

        asyncTest("send mis-ordered", "direct:endpointWithSyncOrdering")
                .inputMessage(text("0"))
                .addExpectation(asyncExpectation("seda:a").expectedMessageCount(1))
                .addExpectation(asyncExpectation("seda:b").expectedMessageCount(1))
                .addExpectation(syncExpectation("seda:s").expectedMessageCount(1));

        syncTest("Test Lenient Processor", "seda:lenient?waitForTaskToComplete=Always")
                .requestBody(text("1"), text("2"), text("3"), text("4"))
                .expectedResponseBody(text("-1"), text("-2"), text("-3"), text("-1"))
                .addExpectation(syncExpectation("seda:lenient").lenient().responseBody(text("-1"), text("-2"), text("-3")));

        syncTest("Match response Lenient Processor", "seda:lenient?waitForTaskToComplete=Always")
                .requestBody(text("1"), text("2"), text("3"), text("4"))
                .requestHeaders(headers(header("5","5")),headers(header("6","6")),headers(header("7","7")),headers(header("8","8")))
                .expectedResponseBody(text("-1"), text("-2"), text("-3"), text("-4"))
                .expectedResponseHeaders(headers(header("-5","-5")),headers(header("-6","-6")),headers(header("-7","-7")),headers(header("-8", "-8")))
                .addExpectation(syncExpectation("seda:lenient").lenient().addProcessors(0,matchedResponse(answer(text("4"), text("-4")),
                        answer(text("3"), text("-3")), answer(text("2"), text("-2")), answer(text("1"), text("-1"))))
                        .addProcessors(0,matchedResponse(headerAnswer(headers(header("8", "8")), headers(header("-8", "-8"))),
                                headerAnswer(headers(header("7", "7")), headers(header("-7", "-7"))),
                                headerAnswer(headers(header("6", "6")), headers(header("-6", "-6"))),
                                headerAnswer(headers(header("5", "5")), headers(header("-5", "-5"))))));

        syncTest("Test Partial Lenient Processor", "seda:partialLenient?waitForTaskToComplete=Always")
                .requestBody(text("1"), text("2"), text("3"), text("4"))
                .expectedResponseBody(text("-1"), text("-2"), text("-3"), text("-4"))
                .addExpectation(syncExpectation("seda:partialLenient").lenient(new Predicate() {
                    @Override
                    public boolean matches(Exchange exchange) {
                        return exchange.getIn().getBody(Integer.class) % 2 == 0;
                    }
                }).responseBody(text("-2"), text("-4")))
                .addExpectation(syncExpectation("seda:partialLenient")
                        .addRepeatedPredicate(new Predicate() {
                            @Override
                            public boolean matches(Exchange exchange) {
                                return exchange.getPattern().equals(ExchangePattern.InOut);
                            }
                        })
                        .expectedBody(text("1"), text("3"))
                        .responseBody(text("-1"), text("-3")));

        syncTest("Test throw receive exceptions","seda:throwsException?waitForTaskToComplete=Always")
                .requestBody(times(5,text("1")))
                .expectedResponseBody(exception(),exception(IOException.class),exception(IOException.class,"foo")
                    ,exception(FileNotFoundException.class),exception(FileNotFoundException.class,"baz"))
                .expectsException()
                .addExpectation(exceptionExpectation("seda:throwsException").expectedMessageCount(1)
                        .exception(new Exception()))
                .addExpectation(exceptionExpectation("seda:throwsException").expectedMessageCount(1)
                        .exception(new IOException()))
                .addExpectation(exceptionExpectation("seda:throwsException").expectedMessageCount(1)
                        .exception(new IOException("foo")))
                .addExpectation(exceptionExpectation("seda:throwsException", FileNotFoundException.class).expectedMessageCount(1))
                .addExpectation(exceptionExpectation("seda:throwsException", FileNotFoundException.class,"baz").expectedMessageCount(1));
    }

}