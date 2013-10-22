package nz.ac.auckland.integration.tests.specification;

import nz.ac.auckland.integration.testing.OrchestratedTestBuilder;
import org.apache.camel.builder.RouteBuilder;

public class OrchestratedTestSubclassTest extends OrchestratedTestBuilder {

    public static void configure() {
        asyncTest("seda:asyncInput","Simple Async Test 1")
                .inputMessage(text("foo"))
                .addExpectation(asyncExpectation("seda:asyncOutput").expectedBody(text("foo")));

        asyncTest("seda:asyncInput","Simple Async Test 2")
                .inputMessage(text("foo"))
                .addExpectation(asyncExpectation("seda:asyncOutput").expectedBody(text("foo")));
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("seda:asyncInput")
                        .to("seda:asyncOutput");
            }
        };
    }
}
