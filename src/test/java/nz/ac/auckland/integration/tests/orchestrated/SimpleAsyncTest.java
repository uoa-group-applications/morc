package nz.ac.auckland.integration.tests.orchestrated;

import nz.ac.auckland.integration.testing.OrchestratedTest;
import nz.ac.auckland.integration.testing.specification.OrchestratedTestSpecification;
import org.apache.camel.builder.RouteBuilder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.List;

import static nz.ac.auckland.integration.testing.dsl.SpecificationBuilderHelper.*;

/**
 * Simple 1 expectation tests for sending and receiving messages using the Camel infrastructure
 */
@RunWith(value = Parameterized.class)
public class SimpleAsyncTest extends OrchestratedTest {

    private static List<OrchestratedTestSpecification> specifications = new ArrayList<>();

    public SimpleAsyncTest(String[] springContextPaths, OrchestratedTestSpecification specification, String testName) {
        super(springContextPaths, specification);
    }

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
    static {
        specifications.add(asyncTest("seda:asyncTestInput", "test async send body")
                .inputMessage(xml("<test/>"))
                .addExpectation(asyncExpectation("seda:asyncTestOutput").expectedBody(xml("<foo/>")))
                .build());

        specifications.add(asyncTest("seda:asyncTestInput", "test async send headers")
                .inputHeaders(headers(headervalue("foo", "baz"), headervalue("abc", "def")))
                .addExpectation(asyncExpectation("seda:asyncTestOutput")
                        .expectedHeaders(headers(headervalue("abc", "def"), headervalue("foo", "baz"))))
                .build());

        specifications.add(asyncTest("seda:asyncTestInputDelayed", "test async delayed")
                .addExpectation(asyncExpectation("seda:asyncTestOutput"))
                .build());
    }

    //this is used by JUnit to initialize each instance of this specification
    @Parameterized.Parameters(name = "{index}: {2}")
    public static java.util.Collection<Object[]> data() {
        List<Object[]> constructorInputs = new ArrayList<>();

        for (OrchestratedTestSpecification spec : specifications) {
            Object[] constructorInput = new Object[]{new String[]{}, spec, spec.getDescription()};
            constructorInputs.add(constructorInput);
        }

        return constructorInputs;
    }
}