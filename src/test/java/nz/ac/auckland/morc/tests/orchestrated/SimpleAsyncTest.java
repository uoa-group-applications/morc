package nz.ac.auckland.morc.tests.orchestrated;

import nz.ac.auckland.morc.MorcTestBuilder;
import nz.ac.auckland.morc.TestBean;
import nz.ac.auckland.morc.resource.HeadersTestResource;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
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
                .input(xml("<test/>"))
                .addMock(asyncMock("seda:asyncTestOutput").expectation(xml("<foo/>")));

        asyncTest("test async send headers", "seda:asyncTestInput")
                .input(headers(header("foo", "baz"), header("abc", "def")))
                .addMock(asyncMock("seda:asyncTestOutput")
                        .expectation(headers(header("abc", "def"), header("foo", "baz"))));

        Map<String, Object> headers = new HashMap<>();
        headers.put("foo", "baz");
        headers.put("abc", "def");

        asyncTest("test async send headers from map", "seda:asyncTestInput")
                .input(new HeadersTestResource(headers))
                .addMock(asyncMock("seda:asyncTestOutput")
                        .expectation(new HeadersTestResource(headers)));

        asyncTest("test async delayed", "seda:asyncTestInputDelayed")
                .input(text("0"))
                .addMock(asyncMock("seda:asyncTestOutput").expectedMessageCount(1));

        asyncTest("Test sender preprocessor applied", "seda:preprocessorSender")
                .input(text("1"))
                .mockFeedPreprocessor(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        exchange.setProperty("preprocessed", true);
                    }
                })
                .addMock(syncMock("seda:preprocessorSender").expectedMessageCount(1)
                ).addPredicates(new Predicate() {
            @Override
            public boolean matches(Exchange exchange) {
                return exchange.getProperty("preprocessed", Boolean.class);
            }
        });

        asyncTest("simple test bean test", new TestBean() {
            @Override
            public void run() throws Exception {
                createCamelContext().createProducerTemplate().send("vm:foo", new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        exchange.getIn().setBody("1");
                    }
                });
            }
        }).addMock(asyncMock("vm:foo").expectation(text("1")));


    }

}