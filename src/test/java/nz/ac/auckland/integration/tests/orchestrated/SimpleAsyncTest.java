package nz.ac.auckland.integration.tests.orchestrated;

import nz.ac.auckland.integration.testing.OrchestratedTestBuilder;
import org.apache.camel.builder.RouteBuilder;

/**
 * Simple 1 expectation tests for sending and receiving messages using the Camel infrastructure
 */
public class SimpleAsyncTest extends OrchestratedTestBuilder {

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("seda:asyncTestInput")
                        .setBody(constant("<foo/>"))
                        .to("seda:asyncTestOutput");

                from("seda:asyncTestInputDelayed")
                        .delay(10000)
                        .to("seda:asyncTestOutput");

            }
        };
    }

    //declare the actual tests here...
    public static void configure() {
        asyncTest("seda:asyncTestInput", "test async send body")
                .inputMessage(xml("<test/>"))
                .addExpectation(asyncExpectation("seda:asyncTestOutput").expectedBody(xml("<foo/>")));

        asyncTest("seda:asyncTestInput", "test async send headers")
                .inputHeaders(headers(headervalue("foo", "baz"), headervalue("abc", "def")))
                .addExpectation(asyncExpectation("seda:asyncTestOutput")
                        .expectedHeaders(headers(headervalue("abc", "def"), headervalue("foo", "baz"))));

        asyncTest("seda:asyncTestInputDelayed", "test async delayed")
                .addExpectation(asyncExpectation("seda:asyncTestOutput"));
    }

}