package nz.ac.auckland.integration.tests.orchestrated;

import nz.ac.auckland.integration.testing.OrchestratedTestBuilder;
import nz.ac.auckland.integration.testing.expectation.MockExpectation;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;


/**
 * Simple 1 expectation synchronous tests for sending and receiving messages using the Camel infrastructure
 */
public class MultiExpectationSyncTest extends OrchestratedTestBuilder {

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:syncInput")
                        .to("seda:asyncTargetInternal?waitForTaskToComplete=Never")
                        .to("seda:syncTarget");

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
                        .to("seda:syncTarget");

                from("direct:syncInputMultiAsyncToSameDest")
                        .to("seda:asyncTargetInternal?waitForTaskToComplete=Never")
                        .to("seda:asyncTargetInternal1?waitForTaskToComplete=Never")
                        .to("seda:asyncTargetInternal1?waitForTaskToComplete=Never")
                        .to("seda:syncTarget");

                from("direct:multiSend")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                template.sendBody("seda:syncMultiSendEndpoint0", exchange.getIn().getBody());
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

    public static void configure() {
        syncTest("direct:syncInput", "Simple send body to two destinations and get correct response")
                .expectedResponseBody(xml("<foo/>"))
                .requestBody(xml("<baz/>"))
                .addExpectation(asyncExpectation("seda:asyncTarget").expectedBody(xml("<async/>")))
                .addExpectation(syncExpectation("seda:syncTarget").expectedBody(xml("<baz/>")).
                        responseBody(xml("<foo/>")));

        syncTest("direct:syncInput", "Simple send body to two destinations with swapped order")
                .expectedResponseBody(xml("<foo/>"))
                .requestBody(xml("<baz/>"))
                .addExpectation(syncExpectation("seda:syncTarget").expectedBody(xml("<baz/>")).
                        responseBody(xml("<foo/>")))
                .addExpectation(asyncExpectation("seda:asyncTarget").expectedBody(xml("<async/>")));

        syncTest("direct:syncInputMultiAsync", "Sync and multiple Async - ensuring total order")
                .expectedResponseBody(xml("<foo/>"))
                .requestBody(xml("<baz/>"))
                        //this expectation will come in last
                .addExpectation(asyncExpectation("seda:asyncTarget1").expectedBody(xml("<async/>")))
                        //this expectation will come in first
                .addExpectation(syncExpectation("seda:syncTarget").expectedBody(xml("<baz/>")).
                        responseBody(xml("<foo/>")))
                        //this expectation will come in second to last
                .addExpectation(asyncExpectation("seda:asyncTarget").expectedBody(xml("<async/>")));

        syncTest("direct:syncInputMultiAsyncToSameDest", "Sync and multiple Async to same dest - ensuring total order")
                .expectedResponseBody(xml("<foo/>"))
                .requestBody(xml("<baz/>"))
                .addExpectation(asyncExpectation("seda:asyncTarget1").expectedBody(xml("<async/>")))
                .addExpectation(syncExpectation("seda:syncTarget").expectedBody(xml("<baz/>")).
                        responseBody(xml("<foo/>")))
                .addExpectation(asyncExpectation("seda:asyncTarget").expectedBody(xml("<async/>")))
                .addExpectation(asyncExpectation("seda:asyncTarget1"));

        syncTest("direct:multiSend", "Send to two sync destinations without total ordering")
                .requestBody(xml("<foo/>"))
                .addExpectation(syncExpectation("seda:syncMultiSendEndpoint0"))
                        //this will receive the message last
                .addExpectation(syncExpectation("seda:syncMultiSendEndpoint1").ordering(MockExpectation.OrderingType.PARTIAL))
                        //this will receive the message first
                .addExpectation(syncExpectation("seda:syncMultiSendEndpoint2").ordering(MockExpectation.OrderingType.PARTIAL));

        syncTest("direct:multiSend1", "Send unordered messages to same sync endpoint without endpoint ordering")
                .requestBody(xml("<foo/>"))
                .addExpectation(syncExpectation("seda:syncMultiSendEndpoint0"))
                        //we will receive this last
                .addExpectation(syncExpectation("seda:syncMultiSendEndpoint1").endpointNotOrdered()
                        .expectedBody(xml("<second/>")))
                        //we will receive this first
                .addExpectation(syncExpectation("seda:syncMultiSendEndpoint1").endpointNotOrdered()
                        .expectedBody(xml("<first/>")));

        syncTest("direct:multiSend2", "Send unordered messages to two different sync destinations without total ordering or endpoint ordering")
                .requestBody(xml("<foo/>"))
                .addExpectation(syncExpectation("seda:syncMultiSendEndpoint0"))
                .addExpectation(syncExpectation("seda:syncMultiSendEndpoint1").endpointNotOrdered().ordering(MockExpectation.OrderingType.PARTIAL)
                        .expectedBody(xml("<fourth/>")))
                .addExpectation(syncExpectation("seda:syncMultiSendEndpoint2").endpointNotOrdered().ordering(MockExpectation.OrderingType.PARTIAL)
                        .expectedBody(xml("<third/>")))
                .addExpectation(syncExpectation("seda:syncMultiSendEndpoint1").endpointNotOrdered().ordering(MockExpectation.OrderingType.PARTIAL)
                        .expectedBody(xml("<second/>")))
                .addExpectation(syncExpectation("seda:syncMultiSendEndpoint2").endpointNotOrdered().ordering(MockExpectation.OrderingType.PARTIAL)
                        .expectedBody(xml("<first/>")));

        syncTest("direct:syncAtEnd", "Send async messages out of order such that sync arrives first")
                .addExpectation(asyncExpectation("seda:a").expectedBody(text("2")).endpointNotOrdered())
                .addExpectation(asyncExpectation("seda:a").expectedBody(text("1")).endpointNotOrdered())
                .addExpectation(syncExpectation(("seda:b")));

        asyncTest("direct:endpointWithSyncOrdering", "send mis-ordered")
                .addExpectation(asyncExpectation("seda:a"))
                .addExpectation(asyncExpectation("seda:b"))
                .addExpectation(syncExpectation("seda:s"));
    }

}