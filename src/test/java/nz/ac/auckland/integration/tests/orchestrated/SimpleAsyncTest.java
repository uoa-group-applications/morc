package nz.ac.auckland.integration.tests.orchestrated;

import nz.ac.auckland.integration.testing.MorcTestBuilder;
import nz.ac.auckland.integration.testing.predicate.HeadersPredicate;
import org.apache.camel.builder.RouteBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple 1 expectation tests for sending and receiving messages using the Camel infrastructure
 */
public class SimpleAsyncTest extends MorcTestBuilder {

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
    @Override
    public void configure() {
        asyncTest("test async send body", "seda:asyncTestInput")
                .inputMessage(xml("<test/>"))
                .addExpectation(asyncExpectation("seda:asyncTestOutput").expectedBody(xml("<foo/>")));

        asyncTest("test async send headers", "seda:asyncTestInput")
                .inputHeaders(headers(header("foo", "baz"), header("abc", "def")))
                .addExpectation(asyncExpectation("seda:asyncTestOutput")
                        .expectedHeaders(headers(header("abc", "def"), header("foo", "baz"))));

        Map<String,Object> headers = new HashMap<>();
        headers.put("foo","baz");
        headers.put("abc","def");

        asyncTest("test async send headers from map", "seda:asyncTestInput")
                        .inputHeaders(headers)
                        .addExpectation(asyncExpectation("seda:asyncTestOutput")
                                .expectedHeaders(headers));

        asyncTest("test async delayed", "seda:asyncTestInputDelayed")
                .inputMessage(text("0"))
                .addExpectation(asyncExpectation("seda:asyncTestOutput").expectedMessageCount(1));


    }

}