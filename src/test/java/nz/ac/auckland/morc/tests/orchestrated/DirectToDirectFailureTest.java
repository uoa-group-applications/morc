package nz.ac.auckland.morc.tests.orchestrated;

import nz.ac.auckland.morc.MorcTestBuilder;
import org.apache.camel.builder.RouteBuilder;

/**
 * I want to illustrate that direct-to-direct routes don't have the
 * fromEndpoint set correctly
 */
public class DirectToDirectFailureTest extends MorcTestBuilder {

    @Override
    public void configure() {
        syncTest("Test direct to direct expectation where Camel doesn't set fromEndpoint properly", "direct:direct2direct")
                .requestBody(xml("<foo/>"))
                .addExpectation(asyncExpectation("direct:directExpectation")
                        .expectedBody(xml("<foo/>")));
    }

    @Override
    public RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                //Camel does the from endpoint in a weird way
                from("direct:direct2direct")
                        .to("direct:directExpectation");
            }
        };
    }

    @Override
    public void runOrchestratedTest() throws Exception {
        AssertionError e = null;
        try {
            super.runOrchestratedTest();
        } catch (AssertionError ex) {
            ex.printStackTrace();
            e = ex;
        }

        assertNotNull(e);
    }
}
