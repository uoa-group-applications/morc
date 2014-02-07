package nz.ac.auckland.integration.tests.orchestrated;

import nz.ac.auckland.integration.testing.MorcTestBuilder;
import nz.ac.auckland.integration.testing.mock.MockDefinition;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;

/**
 * Simple 1 expectation synchronous tests for sending and receiving messages using the Camel infrastructure
 */
public class EachCaseMultiExpectationSyncTest extends MorcTestBuilder {

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:totalOrderOrderedEndpoint")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                template.sendBody("seda:s", "1");
                                Thread.sleep(1000);
                                template.sendBody("seda:s", "2");
                                Thread.sleep(1000);
                                template.sendBody("seda:s", "3");
                            }
                        });

                from("direct:totalOrderUnorderedEndpoint")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                template.sendBody("seda:s", "1");
                                Thread.sleep(1000);
                                template.sendBody("seda:a", "2");
                                Thread.sleep(1000);
                                template.sendBody("seda:a", "1");
                                Thread.sleep(1000);
                                template.sendBody("seda:s", "2");
                            }
                        });

                from("direct:partialOrderOrderedEndpoint")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                template.sendBody("seda:s", "1");
                                Thread.sleep(1000);
                                template.sendBody("seda:s", "2");
                                Thread.sleep(1000);
                                template.sendBody("seda:a", "1");
                                Thread.sleep(1000);
                                template.sendBody("seda:a", "2");
                            }
                        });

                from("direct:partialOrderUnorderedEndpoint")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                template.sendBody("seda:s", "1");
                                Thread.sleep(1000);
                                template.sendBody("seda:a", "4");
                                Thread.sleep(1000);
                                template.sendBody("seda:a", "3");
                                Thread.sleep(1000);
                                template.sendBody("seda:a", "2");
                                Thread.sleep(1000);
                                template.sendBody("seda:a", "1");
                            }
                        });

                from("direct:partialOrderUnorderedEndpoint2")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                template.sendBody("seda:a", "1");
                                Thread.sleep(1000);
                                template.sendBody("seda:s", "1");
                                Thread.sleep(1000);
                                template.sendBody("seda:s", "3");
                                Thread.sleep(1000);
                                template.sendBody("seda:s", "2");

                            }
                        });

                from("vm:partialOrderUnorderedEndpoint3")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                template.sendBody("vm:s", "1");
                                Thread.sleep(1000);
                                template.sendBody("vm:s", "2");
                                Thread.sleep(1000);
                                template.sendBody("vm:s", "3");
                                Thread.sleep(1000);
                                template.sendBody("vm:a", "3");
                                Thread.sleep(1000);
                                template.sendBody("vm:a", "2");
                                Thread.sleep(1000);
                                template.sendBody("vm:a", "1");

                            }
                        });


                from("direct:noOrderOrderedEndpoint")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                template.sendBody("seda:x", "1");
                                Thread.sleep(1000);
                                template.sendBody("seda:s", "1");
                                Thread.sleep(1000);
                                template.sendBody("seda:s", "2");
                                Thread.sleep(1000);
                                template.sendBody("seda:x", "2");
                            }
                        });

                from("direct:noOrderUnorderedEndpoint")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                template.sendBody("seda:a", "2");
                                Thread.sleep(1000);
                                template.sendBody("seda:s", "1");
                                Thread.sleep(1000);
                                template.sendBody("seda:a", "1");
                                Thread.sleep(1000);
                                template.sendBody("seda:s", "2");
                            }
                        });
            }
        };
    }

    @Override
    public void configure() {
        syncTest("Total Order Ordered Endpoint", "direct:totalOrderOrderedEndpoint")
                .requestBody(text("0"))
                .addExpectation(syncExpectation("seda:s").expectedBody(text("1")))
                .addExpectation(syncExpectation("seda:s").expectedBody(text("2")))
                .addExpectation(syncExpectation("seda:s").expectedBody(text("3")));

        syncTest("Total Order Unordered Endpoint", "direct:totalOrderUnorderedEndpoint")
                .requestBody(text("0"))
                .addExpectation(syncExpectation("seda:s").expectedBody(text("1")))
                .addExpectation(asyncExpectation("seda:a").expectedBody(text("2")))
                .addExpectation(asyncExpectation("seda:a").expectedBody(text("1")))
                .addExpectation(syncExpectation("seda:s").expectedBody(text("2")));

        syncTest("Partial Order Ordered Endpoint", "direct:partialOrderOrderedEndpoint")
                .requestBody(text("0"))
                .addExpectation(syncExpectation("seda:s").expectedBody(text("1")))
                .addExpectation(asyncExpectation("seda:a").expectedBody(text("1")))
                .addExpectation(syncExpectation("seda:s").expectedBody(text("2")))
                .addExpectation(asyncExpectation("seda:a").expectedBody(text("2")));

        syncTest("Partial Order Unordered Endpoint", "direct:partialOrderUnorderedEndpoint")
                .requestBody(text("0"))
                .addExpectation(asyncExpectation("seda:a").expectedBody(text("1")).endpointNotOrdered())
                .addExpectation(asyncExpectation("seda:a").expectedBody(text("2")).endpointNotOrdered())
                .addExpectation(asyncExpectation("seda:a").expectedBody(text("3")).endpointNotOrdered())
                .addExpectation(syncExpectation("seda:s").expectedBody(text("1")))
                .addExpectation(asyncExpectation("seda:a").expectedBody(text("4")).endpointNotOrdered());

        syncTest("Partial Order Unordered Endpoint 2", "direct:partialOrderUnorderedEndpoint2")
                .requestBody(text("0"))
                .addExpectation(syncExpectation("seda:s").expectedBody(text("1")).ordering(MockDefinition.OrderingType.PARTIAL).endpointNotOrdered())
                .addExpectation(syncExpectation("seda:s").expectedBody(text("2")).ordering(MockDefinition.OrderingType.PARTIAL).endpointNotOrdered())
                .addExpectation(syncExpectation("seda:s").expectedBody(text("3")).ordering(MockDefinition.OrderingType.PARTIAL).endpointNotOrdered())
                .addExpectation(syncExpectation("seda:a").expectedBody(text("1")).ordering(MockDefinition.OrderingType.PARTIAL));

        asyncTest("Partial Order Unordered Endpoint 3", "vm:partialOrderUnorderedEndpoint3")
                .inputMessage(text("0"))
                .addExpectation(syncExpectation("vm:s").expectedBody(text("1")))
                .addExpectation(syncExpectation("vm:a").expectedBody(text("1")).endpointNotOrdered().ordering(MockDefinition.OrderingType.PARTIAL))
                .addExpectation(syncExpectation("vm:a").expectedBody(text("2")).endpointNotOrdered().ordering(MockDefinition.OrderingType.PARTIAL))
                .addExpectation(syncExpectation("vm:s").expectedBody(text("2")))
                .addExpectation(syncExpectation("vm:s").expectedBody(text("3")))
                .addExpectation(syncExpectation("vm:a").expectedBody(text("3")).endpointNotOrdered().ordering(MockDefinition.OrderingType.PARTIAL));

        syncTest("No Order Ordered Endpoint", "direct:noOrderOrderedEndpoint")
                .requestBody(text("0"))
                .addExpectation(syncExpectation("seda:s").expectedBody(text("1")))
                .addExpectation(syncExpectation("seda:s").expectedBody(text("2")))
                .addExpectation(syncExpectation("seda:x").expectedBody(text("1")).ordering(MockDefinition.OrderingType.NONE))
                .addExpectation(syncExpectation("seda:x").expectedBody(text("2")).ordering(MockDefinition.OrderingType.NONE));

        syncTest("No Order Unordered Endpoint", "direct:noOrderUnorderedEndpoint")
                .requestBody(text("0"))
                .addExpectation(syncExpectation("seda:s").expectedBody(text("1")))
                .addExpectation(syncExpectation("seda:a").expectedBody(text("1")).ordering(MockDefinition.OrderingType.NONE).endpointNotOrdered())
                .addExpectation(syncExpectation("seda:s").expectedBody(text("2")))
                .addExpectation(syncExpectation("seda:a").expectedBody(text("2")).ordering(MockDefinition.OrderingType.NONE).endpointNotOrdered());
    }
}