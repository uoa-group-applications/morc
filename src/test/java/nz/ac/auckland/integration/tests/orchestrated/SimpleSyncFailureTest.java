package nz.ac.auckland.integration.tests.orchestrated;

import nz.ac.auckland.integration.testing.OrchestratedTest;
import nz.ac.auckland.integration.testing.specification.SyncOrchestratedTestSpecification;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static nz.ac.auckland.integration.testing.OrchestratedTestBuilder.*;

public class SimpleSyncFailureTest extends CamelTestSupport {

    Logger logger = LoggerFactory.getLogger(SimpleSyncFailureTest.class);

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

                from("vm:syncInputNoCallouts")
                        .setBody(constant("<abc/>"));

                from("vm:syncInputAsyncOutput")
                        .to("vm:asyncTarget?waitForTaskToComplete=Never")
                        .setBody(constant("<foo/>"));

                from("vm:syncMultiTestPublisher")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                for (int i = 0; i < 3; i++) {
                                    template.sendBody("vm:asyncTarget?waitForTaskToComplete=Never", "<moo/>");
                                }
                            }
                        })
                        .setBody(constant("<foo/>"));

            }
        };
    }

    @Test
    public void testDelayedDeliveryFails() throws Exception {
        SyncOrchestratedTestSpecification spec = new SyncOrchestratedTestSpecification.Builder("vm:syncInputAsyncOutputDelayed", "Test delayed delivery fails")
                .expectedResponseBody(xml("<foo/>"))
                .requestBody(xml("<baz/>"))
                .addExpectation(unreceivedExpectation("vm:somethingToSeeHere"))
                .addExpectation(asyncExpectation("vm:asyncTarget2").expectedBody(xml("<baz/>")))
                .build();

        AssertionError e = null;
        try {
            OrchestratedTest test = new OrchestratedTest(spec);
            test.setUp();
            test.runOrchestratedTest();
        } catch (AssertionError ex) {
            e = ex;
            logger.info("Exception ({}): ", spec.getDescription(), e);
        }
        assertNotNull(e);
    }

    @Test
    public void testInvalidResponseFails() throws Exception {
        SyncOrchestratedTestSpecification spec = new SyncOrchestratedTestSpecification.Builder("vm:syncInputNoCallouts", "Test fails on invalid response")
                .expectedResponseBody(xml("<foo/>"))
                .build();

        AssertionError e = null;
        try {
            OrchestratedTest test = new OrchestratedTest(spec);
            test.setUp();
            test.runOrchestratedTest();
        } catch (AssertionError ex) {
            e = ex;
            logger.info("Exception ({}): ", spec.getDescription(), e);
        }
        assertNotNull(e);
    }

    @Test
    public void testExpectationBodyInvalid() throws Exception {
        SyncOrchestratedTestSpecification spec = new SyncOrchestratedTestSpecification.Builder("vm:syncInputAsyncOutput", "Test fails on invalid expectation body")
                .requestBody(xml("<foo/>"))
                .addExpectation(asyncExpectation("vm:asyncTarget2").expectedBody(xml("<baz/>")))
                .build();

        AssertionError e = null;
        try {
            OrchestratedTest test = new OrchestratedTest(spec);
            test.setUp();
            test.runOrchestratedTest();
        } catch (AssertionError ex) {
            e = ex;
            logger.info("Exception ({}): ", spec.getDescription(), e);

        }
        assertNotNull(e);
    }

    @Test
    public void testExpectationHeadersInvalid() throws Exception {
        SyncOrchestratedTestSpecification spec = new SyncOrchestratedTestSpecification.Builder("vm:syncInputAsyncOutput", "Test fails on invalid expectation headers")
                .requestHeaders(headers(headervalue("foo", "baz")))
                .addExpectation(asyncExpectation("vm:asyncTarget2")
                        .expectedHeaders(headers(headervalue("foo", "baz"), headervalue("abc", "def"))))
                .build();

        AssertionError e = null;
        try {
            OrchestratedTest test = new OrchestratedTest(spec);
            test.setUp();
            test.runOrchestratedTest();
        } catch (AssertionError ex) {
            e = ex;
            logger.info("Exception ({}): ", spec.getDescription(), e);

        }
        assertNotNull(e);
    }

    @Test
    public void testSendMoreExchangesThanExpectations() throws Exception {
        SyncOrchestratedTestSpecification spec = new SyncOrchestratedTestSpecification.Builder("vm:syncMultiTestPublisher", "Test fails on invalid expectation headers")
                .requestBody(xml("<foo/>"))
                .addExpectation(asyncExpectation("vm:asyncTarget")
                        .expectedBody(xml("<foo/>")))
                .build();

        AssertionError e = null;
        try {
            OrchestratedTest test = new OrchestratedTest(spec);
            test.setUp();
            test.runOrchestratedTest();
        } catch (AssertionError ex) {
            e = ex;
            logger.info("Exception ({}): ", spec.getDescription(), e);
        }
        assertNotNull(e);
    }
}