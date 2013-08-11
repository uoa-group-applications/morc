package nz.ac.auckland.integration.tests.orchestrated;

import nz.ac.auckland.integration.testing.OrchestratedTest;
import nz.ac.auckland.integration.testing.expectation.MockExpectation;
import nz.ac.auckland.integration.testing.specification.OrchestratedTestSpecification;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.List;

import static nz.ac.auckland.integration.testing.dsl.SpecificationBuilderHelper.*;

/**
 * Simple 1 expectation synchronous tests for sending and receiving messages using the Camel infrastructure
 */
@RunWith(value = Parameterized.class)
public class EachCaseMultiExpectationSyncTest extends OrchestratedTest {

    private static List<OrchestratedTestSpecification> specifications = new ArrayList<>();

    public EachCaseMultiExpectationSyncTest(String[] springContextPaths, OrchestratedTestSpecification specification, String testName) {
        super(springContextPaths, specification);
    }

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

    static {
        specifications.add(syncTest("direct:totalOrderOrderedEndpoint", "Total Order Ordered Endpoint")
                .addExpectation(syncExpectation("seda:s").expectedBody(text("1")))
                .addExpectation(syncExpectation("seda:s").expectedBody(text("2")))
                .addExpectation(syncExpectation("seda:s").expectedBody(text("3")))
                .build());

        specifications.add(syncTest("direct:totalOrderUnorderedEndpoint", "Total Order Unordered Endpoint")
                .addExpectation(syncExpectation("seda:s").expectedBody(text("1")))
                .addExpectation(asyncExpectation("seda:a").expectedBody(text("2")))
                .addExpectation(asyncExpectation("seda:a").expectedBody(text("1")))
                .addExpectation(syncExpectation("seda:s").expectedBody(text("2")))
                .build());

        specifications.add(syncTest("direct:partialOrderOrderedEndpoint", "Partial Order Ordered Endpoint")
                .addExpectation(syncExpectation("seda:s").expectedBody(text("1")))
                .addExpectation(asyncExpectation("seda:a").expectedBody(text("1")))
                .addExpectation(syncExpectation("seda:s").expectedBody(text("2")))
                .addExpectation(asyncExpectation("seda:a").expectedBody(text("2")))
                .build());

        specifications.add(syncTest("direct:partialOrderUnorderedEndpoint", "Partial Order Unordered Endpoint")
                .addExpectation(asyncExpectation("seda:a").expectedBody(text("1")).endpointNotOrdered())
                .addExpectation(asyncExpectation("seda:a").expectedBody(text("2")).endpointNotOrdered())
                .addExpectation(asyncExpectation("seda:a").expectedBody(text("3")).endpointNotOrdered())
                .addExpectation(syncExpectation("seda:s").expectedBody(text("1")))
                .addExpectation(asyncExpectation("seda:a").expectedBody(text("4")).endpointNotOrdered())
                .build());

        specifications.add(syncTest("direct:partialOrderUnorderedEndpoint2", "Partial Order Unordered Endpoint 2")
                .addExpectation(syncExpectation("seda:s").expectedBody(text("1")).ordering(MockExpectation.OrderingType.PARTIAL).endpointNotOrdered())
                .addExpectation(syncExpectation("seda:s").expectedBody(text("2")).ordering(MockExpectation.OrderingType.PARTIAL).endpointNotOrdered())
                .addExpectation(syncExpectation("seda:s").expectedBody(text("3")).ordering(MockExpectation.OrderingType.PARTIAL).endpointNotOrdered())
                .addExpectation(syncExpectation("seda:a").expectedBody(text("1")).ordering(MockExpectation.OrderingType.PARTIAL))
                .build());

        specifications.add(asyncTest("vm:partialOrderUnorderedEndpoint3", "Partial Order Unordered Endpoint 3")
                .addExpectation(syncExpectation("vm:s").expectedBody(text("1")))
                .addExpectation(syncExpectation("vm:a").expectedBody(text("1")).endpointNotOrdered().ordering(MockExpectation.OrderingType.PARTIAL))
                .addExpectation(syncExpectation("vm:a").expectedBody(text("2")).endpointNotOrdered().ordering(MockExpectation.OrderingType.PARTIAL))
                .addExpectation(syncExpectation("vm:s").expectedBody(text("2")))
                .addExpectation(syncExpectation("vm:s").expectedBody(text("3")))
                .addExpectation(syncExpectation("vm:a").expectedBody(text("3")).endpointNotOrdered().ordering(MockExpectation.OrderingType.PARTIAL))
                .build());

        specifications.add(syncTest("direct:noOrderOrderedEndpoint", "No Order Ordered Endpoint")
                .addExpectation(syncExpectation("seda:s").expectedBody(text("1")))
                .addExpectation(syncExpectation("seda:s").expectedBody(text("2")))
                .addExpectation(syncExpectation("seda:x").expectedBody(text("1")).ordering(MockExpectation.OrderingType.NONE))
                .addExpectation(syncExpectation("seda:x").expectedBody(text("2")).ordering(MockExpectation.OrderingType.NONE))
                .build());

        specifications.add(syncTest("direct:noOrderUnorderedEndpoint", "No Order Unordered Endpoint")
                .addExpectation(syncExpectation("seda:s").expectedBody(text("1")))
                .addExpectation(syncExpectation("seda:a").expectedBody(text("1")).ordering(MockExpectation.OrderingType.NONE).endpointNotOrdered())
                .addExpectation(syncExpectation("seda:s").expectedBody(text("2")))
                .addExpectation(syncExpectation("seda:a").expectedBody(text("2")).ordering(MockExpectation.OrderingType.NONE).endpointNotOrdered())
                .build());

    }

    //this is used by JUnit to initialize each instance of this specification
    @Parameterized.Parameters(name = "{index}: {2}")
    public static java.util.Collection<Object[]> data() {
        List<Object[]> constructorInputs = new ArrayList<>();

        for (OrchestratedTestSpecification spec : specifications) {
            Object[] constructorInput = new Object[]{new String[]{}, spec, spec.getDescription()};
            constructorInputs.add(constructorInput);
        }

        return constructorInputs;
    }
}