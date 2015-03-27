package nz.ac.auckland.morc.tests.orchestrated;

import nz.ac.auckland.morc.MorcMethods;
import nz.ac.auckland.morc.MorcTest;
import nz.ac.auckland.morc.MorcTestBuilder;
import nz.ac.auckland.morc.specification.AsyncOrchestratedTestBuilder;
import nz.ac.auckland.morc.specification.OrchestratedTestSpecification;
import nz.ac.auckland.morc.specification.SyncOrchestratedTestBuilder;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiExpectationSyncFailureTest extends CamelTestSupport implements MorcMethods {

    Logger logger = LoggerFactory.getLogger(MultiExpectationSyncFailureTest.class);

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                //we use VM here because the test will be running in a different context in this case
                from("vm:syncInputAsyncOutputDelayed")
                        .to("vm:asyncTargetDelayed?waitForTaskToComplete=Never")
                        .to("vm:asyncTarget2?waitForTaskToComplete=Never")
                        .setBody(constant("<foo/>"));
                from("vm:asyncTargetDelayed")
                        .delay(10000)
                        .to("vm:somethingToSeeHere");

                from("vm:z")
                        .to("vm:b")
                        .to("vm:b");

                from("vm:outOfOrder")
                        .to("vm:b?waitForTaskToComplete=Always")
                        .to("vm:c");

                from("vm:endpointOutOfOrder")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                template.sendBody("vm:a", "<foo/>");
                                Thread.sleep(1000);
                                template.sendBody("vm:b", "<foo/>");
                                Thread.sleep(1000);
                                template.sendBody("vm:b", "<baz/>");
                            }
                        });

                from("vm:asyncOutOfOrder")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                template.sendBody("vm:b", "");
                                Thread.sleep(1000);
                                template.sendBody("vm:a", "2");
                                Thread.sleep(1000);
                                template.sendBody("vm:a", "1");
                            }
                        });

                from("vm:asyncOutOfOrderSyncEnd")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                template.sendBody("vm:a", "1");
                                Thread.sleep(1000);
                                template.sendBody("vm:a", "2");
                                Thread.sleep(1000);
                                template.sendBody("vm:b", "1");
                            }
                        });

                from("vm:asyncIncorrectBodies")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                template.sendBody("vm:s", "1");
                                Thread.sleep(1000);
                                template.sendBody("vm:a", "4");
                                Thread.sleep(1000);
                                template.sendBody("vm:a", "4");
                                Thread.sleep(1000);
                                template.sendBody("vm:a", "4");
                            }
                        });

                from("vm:noneOrdering")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                template.sendBody("vm:a", "1");
                                Thread.sleep(1000);
                                template.sendBody("vm:s", "1");
                                for (int i = 0; i < 3; i++) {
                                    template.sendBody("vm:n", "1");
                                    Thread.sleep(1000);
                                }
                            }
                        });
            }
        };
    }

    private void runTest(OrchestratedTestSpecification spec) throws Exception {
        AssertionError e = null;
        try {
            MorcTest test = new MorcTest(spec);
            test.setUp();
            test.runOrchestratedTest();
        } catch (AssertionError ex) {
            e = ex;
            logger.info("Exception ({}): ", spec.getDescription(), e);
        }
        assertNotNull(e);
    }

    public MorcTestBuilder createMorcTestBuilder() {
        return new MorcTestBuilder() {
            @Override
            protected void configure() {

            }
        };
    }

    @Test
    public void testExpectationToTwoEndpointsSendTwoToOneFails() throws Exception {
        MorcTestBuilder morcMethods = createMorcTestBuilder();

        OrchestratedTestSpecification spec = new AsyncOrchestratedTestBuilder("Test no exchange available fails", "vm:z")
                .input(text("0"))
                .addMock(morcMethods.asyncMock("vm:b"))
                .addMock(morcMethods.asyncMock("vm:c"))
                .build();

        runTest(spec);
    }

    @Test
    public void testDelayedDeliveryFails() throws Exception {
        MorcTestBuilder morcMethods = createMorcTestBuilder();

        OrchestratedTestSpecification spec = new SyncOrchestratedTestBuilder("Test delayed delivery fails", "vm:syncInputAsyncOutputDelayed")
                .expectation(xml("<foo/>"))
                .request(xml("<baz/>"))
                .addMock(morcMethods.unreceivedMock("vm:somethingToSeeHere").expectedMessageCount(0))
                .addMock(morcMethods.asyncMock("vm:asyncTarget2").expectation(xml("<baz/>")))
                .build();

        runTest(spec);
    }

    @Test
    public void testOutOfOrderDeliveryTotallyOrderedFails() throws Exception {
        MorcTestBuilder morcMethods = createMorcTestBuilder();

        OrchestratedTestSpecification spec = new AsyncOrchestratedTestBuilder("Test out of order expectations to different endpoints fails", "vm:outOfOrder")
                .input(text("0"))
                .addMock(morcMethods.syncMock("vm:c"))
                .addMock(morcMethods.syncMock("vm:b"))
                .build();

        runTest(spec);
    }

    @Test
    public void testOutOfOrderDeliveryNotTotallyOrderedEndpointOrderedFails() throws Exception {
        MorcTestBuilder morcMethods = createMorcTestBuilder();

        OrchestratedTestSpecification spec = new AsyncOrchestratedTestBuilder("Test out of order expectations to different endpoints fails", "vm:endpointOutOfOrder")
                .input(text("0"))
                .addMock(morcMethods.syncMock("vm:a").expectation(xml("<foo/>")))
                .addMock(morcMethods.asyncMock("vm:b").expectation(xml("<baz/>")))
                .addMock(morcMethods.asyncMock("vm:b").expectation(xml("<foo/>")))
                .build();

        runTest(spec);
    }

    @Test
    public void testMultiAsyncEndpointFails() throws Exception {
        MorcTestBuilder morcMethods = createMorcTestBuilder();

        OrchestratedTestSpecification spec = new AsyncOrchestratedTestBuilder("Test out of order async expectations fail", "vm:asyncOutOfOrder")
                .input(text("0"))
                .addMock(morcMethods.asyncMock("vm:a").expectation(text("1")))
                .addMock(morcMethods.asyncMock("vm:b"))
                .addMock(morcMethods.asyncMock("vm:a").expectation(text("2")))
                .build();

        runTest(spec);
    }


    @Test
    public void testMultiAsyncEndpointEndOfQueueFails() throws Exception {
        MorcTestBuilder morcMethods = createMorcTestBuilder();

        OrchestratedTestSpecification spec = new AsyncOrchestratedTestBuilder("Test out of order async expectations fail", "vm:asyncOutOfOrder")
                .input(text("0"))
                .addMock(morcMethods.asyncMock("vm:a").expectation(text("1")))
                .addMock(morcMethods.asyncMock("vm:a").expectation(text("2")))
                .addMock(morcMethods.asyncMock("vm:b"))
                .build();

        runTest(spec);
    }


    @Test
    public void testEndpointUnorderedIncorrectBodies() throws Exception {
        MorcTestBuilder morcMethods = createMorcTestBuilder();

        OrchestratedTestSpecification spec = new AsyncOrchestratedTestBuilder("Test out of order async expectations fail with unordered endpoint", "vm:asyncIncorrectBodies")
                .input(text("0"))
                .addMock(morcMethods.asyncMock("vm:a").expectation(text("1")).endpointNotOrdered())
                .addMock(morcMethods.asyncMock("vm:a").expectation(text("2")).endpointNotOrdered())
                .addMock(morcMethods.asyncMock("vm:s"))
                .addMock(morcMethods.asyncMock("vm:a").expectation(text("3")).endpointNotOrdered())
                .build();

        runTest(spec);
    }

    @Test
    public void testNoneOrderingFailure() throws Exception {
        MorcTestBuilder morcMethods = createMorcTestBuilder();

        OrchestratedTestSpecification spec = new AsyncOrchestratedTestBuilder("Test none ordering handled first during order validation", "vm:noneOrdering")
                .input(text("0"))
                .addMock(morcMethods.syncMock("vm:s").expectation(text("1")))
                .addMock(morcMethods.asyncMock("vm:a").expectation(text("1")))
                .addMock(morcMethods.asyncMock("vm:n").expectationMultiplier(3, text("1")).ordering(morcMethods.noOrdering()))
                .build();

        runTest(spec);
    }

}