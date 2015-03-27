package nz.ac.auckland.morc.tests.orchestrated;

import nz.ac.auckland.morc.MorcTestBuilder;
import nz.ac.auckland.morc.mock.MockDefinition;
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
                .request(text("0"))
                .addMock(syncMock("seda:s").expectation(text("1")))
                .addMock(syncMock("seda:s").expectation(text("2")))
                .addMock(syncMock("seda:s").expectation(text("3")));

        syncTest("Total Order Unordered Endpoint", "direct:totalOrderUnorderedEndpoint")
                .request(text("0"))
                .addMock(syncMock("seda:s").expectation(text("1")))
                .addMock(asyncMock("seda:a").expectation(text("2")))
                .addMock(asyncMock("seda:a").expectation(text("1")))
                .addMock(syncMock("seda:s").expectation(text("2")));

        syncTest("Partial Order Ordered Endpoint", "direct:partialOrderOrderedEndpoint")
                .request(text("0"))
                .addMock(syncMock("seda:s").expectation(text("1")))
                .addMock(asyncMock("seda:a").expectation(text("1")))
                .addMock(syncMock("seda:s").expectation(text("2")))
                .addMock(asyncMock("seda:a").expectation(text("2")));

        syncTest("Partial Order Unordered Endpoint", "direct:partialOrderUnorderedEndpoint")
                .request(text("0"))
                .addMock(asyncMock("seda:a").expectation(text("1")).endpointNotOrdered())
                .addMock(asyncMock("seda:a").expectation(text("2")).endpointNotOrdered())
                .addMock(asyncMock("seda:a").expectation(text("3")).endpointNotOrdered())
                .addMock(syncMock("seda:s").expectation(text("1")))
                .addMock(asyncMock("seda:a").expectation(text("4")).endpointNotOrdered());

        syncTest("Partial Order Unordered Endpoint 2", "direct:partialOrderUnorderedEndpoint2")
                .request(text("0"))
                .addMock(syncMock("seda:s").expectation(text("1")).ordering(MockDefinition.OrderingType.PARTIAL).endpointNotOrdered())
                .addMock(syncMock("seda:s").expectation(text("2")).ordering(MockDefinition.OrderingType.PARTIAL).endpointNotOrdered())
                .addMock(syncMock("seda:s").expectation(text("3")).ordering(MockDefinition.OrderingType.PARTIAL).endpointNotOrdered())
                .addMock(syncMock("seda:a").expectation(text("1")).ordering(MockDefinition.OrderingType.PARTIAL));

        asyncTest("Partial Order Unordered Endpoint 3", "vm:partialOrderUnorderedEndpoint3")
                .input(text("0"))
                .addMock(syncMock("vm:s").expectation(text("1")))
                .addMock(syncMock("vm:a").expectation(text("1")).endpointNotOrdered().ordering(MockDefinition.OrderingType.PARTIAL))
                .addMock(syncMock("vm:a").expectation(text("2")).endpointNotOrdered().ordering(MockDefinition.OrderingType.PARTIAL))
                .addMock(syncMock("vm:s").expectation(text("2")))
                .addMock(syncMock("vm:s").expectation(text("3")))
                .addMock(syncMock("vm:a").expectation(text("3")).endpointNotOrdered().ordering(MockDefinition.OrderingType.PARTIAL));

        syncTest("No Order Ordered Endpoint", "direct:noOrderOrderedEndpoint")
                .request(text("0"))
                .addMock(syncMock("seda:s").expectation(text("1")))
                .addMock(syncMock("seda:s").expectation(text("2")))
                .addMock(syncMock("seda:x").expectation(text("1")).ordering(MockDefinition.OrderingType.NONE))
                .addMock(syncMock("seda:x").expectation(text("2")).ordering(MockDefinition.OrderingType.NONE));

        syncTest("No Order Unordered Endpoint", "direct:noOrderUnorderedEndpoint")
                .request(text("0"))
                .addMock(syncMock("seda:s").expectation(text("1")))
                .addMock(syncMock("seda:a").expectation(text("1")).ordering(MockDefinition.OrderingType.NONE).endpointNotOrdered())
                .addMock(syncMock("seda:s").expectation(text("2")))
                .addMock(syncMock("seda:a").expectation(text("2")).ordering(MockDefinition.OrderingType.NONE).endpointNotOrdered());
    }
}