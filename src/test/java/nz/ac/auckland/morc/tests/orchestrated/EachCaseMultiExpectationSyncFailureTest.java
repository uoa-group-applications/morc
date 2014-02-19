package nz.ac.auckland.morc.tests.orchestrated;

import nz.ac.auckland.morc.MorcTest;
import nz.ac.auckland.morc.mock.MockDefinition;
import nz.ac.auckland.morc.specification.AsyncOrchestratedTestBuilder;
import nz.ac.auckland.morc.specification.OrchestratedTestSpecification;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.seda.SedaEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Collection;

import static nz.ac.auckland.morc.MorcTestBuilder.*;

public class EachCaseMultiExpectationSyncFailureTest extends CamelTestSupport {

    Logger logger = LoggerFactory.getLogger(EachCaseMultiExpectationSyncFailureTest.class);

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {

                from("vm:totalOrderOrderedEndpoint")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                template.sendBody("vm:s", "1");
                                Thread.sleep(1000);
                                template.sendBody("vm:s", "3");
                                Thread.sleep(1000);
                                template.sendBody("vm:s", "2");
                            }
                        });

                from("vm:totalOrderUnorderedEndpoint")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                template.sendBody("vm:s", "1");
                                Thread.sleep(1000);
                                template.sendBody("vm:a", "2");
                                Thread.sleep(1000);
                                template.sendBody("vm:s", "2");
                                Thread.sleep(1000);
                                template.sendBody("vm:a", "1");
                            }
                        });

                from("vm:partialOrderOrderedEndpoint")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                template.sendBody("vm:s", "1");
                                Thread.sleep(1000);
                                template.sendBody("vm:s", "2");
                                Thread.sleep(1000);
                                template.sendBody("vm:a", "2");
                                Thread.sleep(1000);
                                template.sendBody("vm:a", "1");
                            }
                        });

                from("vm:partialOrderUnorderedEndpoint")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                template.sendBody("vm:a", "2");
                                Thread.sleep(1000);
                                template.sendBody("vm:a", "1");
                                Thread.sleep(1000);
                                template.sendBody("vm:s", "1");
                                Thread.sleep(1000);
                                template.sendBody("vm:s", "2");
                            }
                        });

                from("vm:partialOrderUnorderedEndpoint2")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                template.sendBody("vm:a", "1");
                                Thread.sleep(1000);
                                template.sendBody("vm:a", "1");
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
                                template.sendBody("vm:a", "1");
                                Thread.sleep(1000);
                                template.sendBody("vm:a", "1");

                            }
                        });


                from("vm:testNoOrderOrderedEndpoint")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                template.sendBody("vm:x", "1");
                                Thread.sleep(1000);
                                template.sendBody("vm:x", "1");
                            }
                        });

                from("vm:testNoOrderOrderedEndpoint2")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                template.sendBody("vm:x", "1");
                                Thread.sleep(1000);
                                template.sendBody("vm:x", "2");
                                Thread.sleep(1000);
                                template.sendBody("vm:s", "2");
                                Thread.sleep(1000);
                                template.sendBody("vm:s", "1");
                            }
                        });

                from("vm:testNoOrderUnorderedEndpoint")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                template.sendBody("vm:x", "2");
                                Thread.sleep(1000);
                                template.sendBody("vm:x", "1");
                                Thread.sleep(1000);
                                template.sendBody("vm:s", "2");
                                Thread.sleep(1000);
                                template.sendBody("vm:s", "1");
                            }
                        });

                from("vm:testNoOrderUnorderedEndpoint2")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                template.sendBody("vm:x", "1");
                                Thread.sleep(1000);
                                template.sendBody("vm:x", "1");
                                Thread.sleep(1000);
                            }
                        });
            }
        };
    }

    private void runTest(OrchestratedTestSpecification spec) throws Exception {

        Collection<Endpoint> endpoints = context.getEndpoints();
        for (Endpoint endpoint : endpoints) {
            if (endpoint instanceof SedaEndpoint) ((SedaEndpoint) endpoint).setPurgeWhenStopping(true);
        }

        AssertionError e = null;
        try {
            MorcTest test = new MorcTest(spec);
            test.setUp();
            test.runOrchestratedTest();
        } catch (AssertionError ex) {
            if (!ex.getMessage().contains("Received message count. Expected"))
                e = ex;

            logger.info("Exception ({}): ", spec.getDescription(), e);
        }
        assertNotNull(e);
    }

    @DirtiesContext
    @Test
    public void testTotalOrderOrderedEndpoint() throws Exception {
        OrchestratedTestSpecification spec = new AsyncOrchestratedTestBuilder("Total Order Ordered Endpoint", "vm:totalOrderOrderedEndpoint")
                .inputMessage(text("0"))
                .addExpectation(syncExpectation("vm:s").expectedBody(text("1")))
                .addExpectation(syncExpectation("vm:s").expectedBody(text("2")))
                .addExpectation(syncExpectation("vm:s").expectedBody(text("3")))
                .build();

        runTest(spec);
    }

    @DirtiesContext
    @Test
    public void testTotalOrderUnOrderedEndpoint() throws Exception {
        OrchestratedTestSpecification spec = new AsyncOrchestratedTestBuilder("Total Order Unordered Endpoint", "vm:totalOrderUnorderedEndpoint")
                .inputMessage(text("0"))
                .addExpectation(syncExpectation("vm:s").expectedBody(text("1")))
                .addExpectation(syncExpectation("vm:a").expectedBody(text("1")).endpointNotOrdered())
                .addExpectation(syncExpectation("vm:a").expectedBody(text("2")).endpointNotOrdered())
                .addExpectation(syncExpectation("vm:s").expectedBody(text("2")))
                .build();

        runTest(spec);
    }

    @DirtiesContext
    @Test
    public void testPartialOrderOrderedEndpoint() throws Exception {
        OrchestratedTestSpecification spec = new AsyncOrchestratedTestBuilder("Partial Order Ordered Endpoint", "vm:partialOrderOrderedEndpoint")
                .inputMessage(text("0"))
                .addExpectation(syncExpectation("vm:s").expectedBody(text("1")))
                .addExpectation(asyncExpectation("vm:a").expectedBody(text("1")))
                .addExpectation(syncExpectation("vm:s").expectedBody(text("2")))
                .addExpectation(asyncExpectation("vm:a").expectedBody(text("2")))
                .build();

        runTest(spec);
    }

    @DirtiesContext
    @Test
    public void testPartialOrderUnorderedEndpoint() throws Exception {
        OrchestratedTestSpecification spec = new AsyncOrchestratedTestBuilder("Partial Order Unordered Endpoint", "vm:partialOrderUnorderedEndpoint")
                .inputMessage(text("0"))
                .addExpectation(syncExpectation("vm:s").expectedBody(text("1")))
                .addExpectation(syncExpectation("vm:s").expectedBody(text("2")))
                .addExpectation(syncExpectation("vm:a").expectedBody(text("1")).endpointNotOrdered())
                .addExpectation(syncExpectation("vm:a").expectedBody(text("2")).endpointNotOrdered())
                .build();

        runTest(spec);
    }

    @DirtiesContext
    @Test
    public void testPartialOrderUnorderedEndpoint2() throws Exception {
        OrchestratedTestSpecification spec = new AsyncOrchestratedTestBuilder("Partial Order Unordered Endpoint 2", "vm:partialOrderUnorderedEndpoint2")
                .inputMessage(text("0"))
                .addExpectation(syncExpectation("vm:a").expectedBody(text("1")).endpointNotOrdered())
                .addExpectation(syncExpectation("vm:a").expectedBody(text("2")).endpointNotOrdered())
                .build();

        runTest(spec);
    }

    @DirtiesContext
    @Test
    public void testPartialOrderUnorderedEndpoint3() throws Exception {
        OrchestratedTestSpecification spec = new AsyncOrchestratedTestBuilder("Partial Order Unordered Endpoint 3", "vm:partialOrderUnorderedEndpoint3")
                .inputMessage(text("0"))
                .addExpectation(syncExpectation("vm:s").expectedBody(text("1")))
                .addExpectation(syncExpectation("vm:a").expectedBody(text("1")).endpointNotOrdered().ordering(MockDefinition.OrderingType.PARTIAL))
                .addExpectation(syncExpectation("vm:a").expectedBody(text("2")).endpointNotOrdered().ordering(MockDefinition.OrderingType.PARTIAL))
                .addExpectation(syncExpectation("vm:s").expectedBody(text("2")))
                .addExpectation(syncExpectation("vm:s").expectedBody(text("3")))
                .addExpectation(syncExpectation("vm:a").expectedBody(text("3")).endpointNotOrdered().ordering(MockDefinition.OrderingType.PARTIAL))
                .build();

        runTest(spec);
    }

    @DirtiesContext
    @Test
    public void testNoOrderOrderedEndpoint() throws Exception {
        OrchestratedTestSpecification spec = new AsyncOrchestratedTestBuilder("No Order Ordered Endpoint", "vm:testNoOrderOrderedEndpoint")
                .inputMessage(text("0"))
                .addExpectation(syncExpectation("vm:x").expectedBody(text("1")).ordering(MockDefinition.OrderingType.NONE))
                .addExpectation(syncExpectation("vm:x").expectedBody(text("2")).ordering(MockDefinition.OrderingType.NONE))
                .build();

        runTest(spec);
    }

    @DirtiesContext
    @Test
    public void testNoOrderOrderedEndpoint2() throws Exception {
        OrchestratedTestSpecification spec = new AsyncOrchestratedTestBuilder("No Order Ordered Endpoint 2", "vm:testNoOrderOrderedEndpoint2")
                .inputMessage(text("0"))
                .addExpectation(syncExpectation("vm:s").expectedBody(text("1")))
                .addExpectation(syncExpectation("vm:x").expectedBody(text("1")).ordering(MockDefinition.OrderingType.NONE))
                .addExpectation(syncExpectation("vm:s").expectedBody(text("2")))
                .addExpectation(syncExpectation("vm:x").expectedBody(text("2")).ordering(MockDefinition.OrderingType.NONE))
                .build();

        runTest(spec);
    }

    @DirtiesContext
    @Test
    public void testNoOrderUnorderedEndpoint() throws Exception {
        OrchestratedTestSpecification spec = new AsyncOrchestratedTestBuilder("No Order Unordered Endpoint", "vm:testNoOrderUnorderedEndpoint")
                .inputMessage(text("0"))
                .addExpectation(syncExpectation("vm:s").expectedBody(text("1")))
                .addExpectation(syncExpectation("vm:s").expectedBody(text("2")))
                .addExpectation(syncExpectation("vm:x").expectedBody(text("1")).ordering(MockDefinition.OrderingType.NONE).endpointNotOrdered())
                .addExpectation(syncExpectation("vm:x").expectedBody(text("2")).ordering(MockDefinition.OrderingType.NONE).endpointNotOrdered())
                .build();

        runTest(spec);
    }

    @DirtiesContext
    @Test
    public void testNoOrderUnorderedEndpoint2() throws Exception {
        OrchestratedTestSpecification spec = new AsyncOrchestratedTestBuilder("No Order Unordered Endpoint 2", "vm:testNoOrderUnorderedEndpoint2")
                .inputMessage(text("0"))
                .addExpectation(syncExpectation("vm:x").expectedBody(text("1")).ordering(MockDefinition.OrderingType.NONE).endpointNotOrdered())
                .addExpectation(syncExpectation("vm:x").expectedBody(text("2")).ordering(MockDefinition.OrderingType.NONE).endpointNotOrdered())
                .build();

        runTest(spec);
    }

}