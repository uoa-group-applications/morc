package nz.ac.auckland.morc.tests.orchestrated;

import nz.ac.auckland.morc.MorcMethods;
import nz.ac.auckland.morc.MorcTest;
import nz.ac.auckland.morc.MorcTestBuilder;
import nz.ac.auckland.morc.resource.HeadersTestResource;
import nz.ac.auckland.morc.specification.OrchestratedTestSpecification;
import nz.ac.auckland.morc.specification.SyncOrchestratedTestBuilder;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SimpleSyncFailureTest extends CamelTestSupport implements MorcMethods {

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

                from("vm:syncInputSyncOutput")
                        .to("vm:syncTarget?waitForTaskToComplete=Always");

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

    public MorcTestBuilder createMorcTestBuilder() {
        return new MorcTestBuilder() {
            @Override
            protected void configure() {

            }
        };
    }

    @Test
    public void testDelayedDeliveryFails() throws Exception {
        MorcTestBuilder morcMethods = createMorcTestBuilder();

        OrchestratedTestSpecification spec = new SyncOrchestratedTestBuilder("Test delayed delivery fails",
                "vm:syncInputAsyncOutputDelayed")
                .expectation(xml("<foo/>"))
                .request(xml("<baz/>"))
                .addMock(morcMethods.unreceivedMock("vm:somethingToSeeHere"))
                .addMock(morcMethods.asyncMock("vm:asyncTarget2").expectation(xml("<baz/>")))
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
        MorcTestBuilder morcMethods = createMorcTestBuilder();

        OrchestratedTestSpecification spec = new SyncOrchestratedTestBuilder("Test fails on invalid response", "vm:syncInputNoCallouts")
                .request(text("0"))
                .expectation(xml("<foo/>"))
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
        MorcTestBuilder morcMethods = createMorcTestBuilder();

        OrchestratedTestSpecification spec = new SyncOrchestratedTestBuilder("Test fails on invalid expectation body", "vm:syncInputAsyncOutput")
                .request(xml("<foo/>"))
                .addMock(morcMethods.asyncMock("vm:asyncTarget").expectation(xml("<baz/>")))
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
        MorcTestBuilder morcMethods = createMorcTestBuilder();

        OrchestratedTestSpecification spec = new SyncOrchestratedTestBuilder("Test fails on invalid expectation headers", "vm:syncInputAsyncOutput")
                .request(headers(new HeaderValue("foo", "baz")))
                .addMock(morcMethods.asyncMock("vm:asyncTarget")
                        .expectation(headers(new HeaderValue("foo", "baz"), new HeaderValue("abc", "def"))))
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
        MorcTestBuilder morcMethods = createMorcTestBuilder();

        OrchestratedTestSpecification spec = new SyncOrchestratedTestBuilder("Test fails on more exchanges than expectations",
                "vm:syncMultiTestPublisher")
                .request(xml("<foo/>"))
                .addMock(morcMethods.asyncMock("vm:asyncTarget3")
                        .expectation(xml("<moo/>")))
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
        MorcTestBuilder morcMethods = createMorcTestBuilder();

        Map<String, Object> headers = new HashMap<>();
        headers.put("abc", "123");
        headers.put("foo", "baz");

        OrchestratedTestSpecification spec = new SyncOrchestratedTestBuilder("Test unexpected response headers",
                "vm:syncHeaderResponse")
                .request(text("0"))
                .expectation(new HeadersTestResource(headers))
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
        MorcTestBuilder morcMethods = createMorcTestBuilder();

        OrchestratedTestSpecification spec = new SyncOrchestratedTestBuilder("Test Exception Found but not expected",
                "vm:exceptionThrower")
                .request(text("foo"))
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
        MorcTestBuilder morcMethods = createMorcTestBuilder();

        IllegalArgumentException e = null;
        try {
            BadOrchestratedTest test = new BadOrchestratedTest();
            test.setUp();
            test.runOrchestratedTest();
        } catch (IllegalArgumentException ex) {
            e = ex;
        }
        assertNotNull(e);

    }

    @Test
    public void testNegativeSendIntervalError() throws Exception {
        MorcTestBuilder morcMethods = createMorcTestBuilder();

        IllegalArgumentException e = null;

        try {
            OrchestratedTestSpecification spec = new SyncOrchestratedTestBuilder("Test Exception Found but not expected",
                    "vm:exceptionThrower")
                    .request(text("foo"))
                    .sendInterval(-1000)
                    .build();
        } catch (IllegalArgumentException ex) {
            e = ex;
        }

        assertNotNull(e);
    }

    @Test
    public void testNoExpectedMessageCountSetForExpectation() throws Exception {
        MorcTestBuilder morcMethods = createMorcTestBuilder();

        AssertionError e = null;

        try {
            OrchestratedTestSpecification spec = new SyncOrchestratedTestBuilder("Test response with no expectation predicates",
                    "vm:syncInputSyncOutput")
                    .requestMultiplier(3, xml("<baz/>"))
                    .addMock(morcMethods.syncMock("vm:syncTarget")
                            .responseMultiplier(3, xml("<foo/>")))
                    .sendInterval(3000)
                    .expectationMultiplier(3, xml("<foo/>")).build();

            MorcTest test = new MorcTest(spec);
            test.setUp();
            test.runOrchestratedTest();
        } catch (AssertionError ex) {
            logger.info("Encountered exception: ", ex);
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