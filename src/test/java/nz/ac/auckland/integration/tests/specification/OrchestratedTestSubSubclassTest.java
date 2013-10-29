package nz.ac.auckland.integration.tests.specification;

import org.apache.camel.builder.RouteBuilder;

//A test to show what happens with 'inheritance' of configure()
public class OrchestratedTestSubSubclassTest extends OrchestratedTestSubclassTest {

    public static void configure() {
        syncTest("direct:syncInput","Simple Sync Test 1")
                .requestBody(text("foo"))
                .expectedResponseBody(text("baz"));

        syncTest("direct:syncInput","Simple Sync Test 2")
                .requestBody(text("foo"))
                .expectedResponseBody(text("baz"));
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        RouteBuilder rb = super.createRouteBuilder();

        rb.includeRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:syncInput")
                        .transform(constant("baz"));
            }
        });

        return rb;
    }
}
