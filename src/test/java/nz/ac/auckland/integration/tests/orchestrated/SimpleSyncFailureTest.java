package nz.ac.auckland.integration.tests.orchestrated;

import nz.ac.auckland.integration.testing.MorcTest;
import nz.ac.auckland.integration.testing.resource.HeadersTestResource;
import nz.ac.auckland.integration.testing.specification.OrchestratedTestSpecification;
import nz.ac.auckland.integration.testing.specification.SyncOrchestratedTestBuilder;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static nz.ac.auckland.integration.testing.MorcTestBuilder.*;

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
                                    template.sendBody("vm:asyncTarget3?waitForTaskToComplete=Never", "<moo/>");
                                }
                            }
                        })
                        .setBody(constant("<foo/>"));

                from("vm:syncHeaderResponse")
                        .setHeader("baz", constant("foo"))
                        .setHeader("123", constant("abc"));

                from("vm:exceptionThrower")
                        .throwException(new IOException());


            }
        };
    }

    @Test
    public void testDelayedDeliveryFails() throws Exception {
        OrchestratedTestSpecification spec = new SyncOrchestratedTestBuilder("Test delayed delivery fails",
                "vm:syncInputAsyncOutputDelayed")
                .expectedResponseBody(xml("<foo/>"))
                .requestBody(xml("<baz/>"))
                .addExpectation(unreceivedExpectation("vm:somethingToSeeHere"))
                .addExpectation(asyncExpectation("vm:asyncTarget2").expectedBody(xml("<baz/>")))
                .build();

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

    @Test
    public void testInvalidResponseFails() throws Exception {
        OrchestratedTestSpecification spec = new SyncOrchestratedTestBuilder("Test fails on invalid response","vm:syncInputNoCallouts")
                .expectedResponseBody(xml("<foo/>"))
                .build();

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

    @Test
    public void testExpectationBodyInvalid() throws Exception {
        OrchestratedTestSpecification spec = new SyncOrchestratedTestBuilder("Test fails on invalid expectation body","vm:syncInputAsyncOutput")
                .requestBody(xml("<foo/>"))
                .addExpectation(asyncExpectation("vm:asyncTarget").expectedBody(xml("<baz/>")))
                .build();

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

    @Test
    public void testExpectationHeadersInvalid() throws Exception {
        OrchestratedTestSpecification spec = new SyncOrchestratedTestBuilder("Test fails on invalid expectation headers","vm:syncInputAsyncOutput")
                .requestHeaders(headers(new HeaderValue("foo", "baz")))
                .addExpectation(asyncExpectation("vm:asyncTarget")
                        .expectedHeaders(headers(new HeaderValue("foo", "baz"), new HeaderValue("abc", "def"))))
                .build();

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

    @Test
    public void testSendMoreExchangesThanExpectations() throws Exception {
        OrchestratedTestSpecification spec = new SyncOrchestratedTestBuilder("Test fails on more exchanges than expectations",
                "vm:syncMultiTestPublisher")
                .requestBody(xml("<foo/>"))
                .addExpectation(asyncExpectation("vm:asyncTarget3")
                        .expectedBody(xml("<moo/>")))
                .build();

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

    @Test
    public void testResponseHeadersInvalid() throws Exception {

        Map<String, Object> headers = new HashMap<>();
        headers.put("abc", "123");
        headers.put("foo", "baz");

        OrchestratedTestSpecification spec = new SyncOrchestratedTestBuilder("Test unexpected response headers",
                "vm:syncHeaderResponse")
                .expectedResponseHeaders(new HeadersTestResource(headers))
                .build();

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

    @Test
    public void testExceptionFoundButUnexpectedWithNoValidator() throws Exception {
        OrchestratedTestSpecification spec = new SyncOrchestratedTestBuilder("Test Exception Found but not expected",
                "vm:exceptionThrower")
                .requestBody(text("foo"))
                .build();

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

    @Test
    public void testNoSpecificationEntered() throws Exception {

        IllegalStateException e = null;
        try {
            BadOrchestratedTest test = new BadOrchestratedTest();
            test.setUp();
            test.runOrchestratedTest();
        } catch (IllegalStateException ex) {
            e = ex;
        }
        assertNotNull(e);

    }

    class BadOrchestratedTest extends MorcTest {
        public BadOrchestratedTest() {
            //we never set the spec!
        }
    }

}