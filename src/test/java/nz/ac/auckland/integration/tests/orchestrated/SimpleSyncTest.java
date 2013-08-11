package nz.ac.auckland.integration.tests.orchestrated;

import nz.ac.auckland.integration.testing.OrchestratedTest;
import nz.ac.auckland.integration.testing.expectation.MockExpectation;
import nz.ac.auckland.integration.testing.specification.OrchestratedTestSpecification;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.List;

import static nz.ac.auckland.integration.testing.dsl.SpecificationBuilderHelper.*;

/**
 * Simple 1 expectation synchronous tests for sending and receiving messages using the Camel infrastructure
 */
@RunWith(value = Parameterized.class)
public class SimpleSyncTest extends OrchestratedTest {

    protected static List<OrchestratedTestSpecification> specifications = new ArrayList<>();

    public SimpleSyncTest(String[] springContextPaths, OrchestratedTestSpecification specification, String testName) {
        super(springContextPaths, specification);
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:syncInputAsyncOutput")
                        .to("seda:asyncTarget?waitForTaskToComplete=Never")
                        .setBody(constant("<foo/>"));

                //we use seda so that the from endpoint changes
                from("direct:syncInputSyncOutput")
                        .to("seda:syncTarget");

                from("direct:syncMultiTestPublisher")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                for (int i = 0; i < 3; i++) {
                                    template.sendBody("seda:asyncTarget", "<moo/>");
                                }
                            }
                        })
                        .setBody(constant("<foo/>"));

                from("direct:syncInputNoCallouts")
                        .setBody(constant("<abc/>"));
            }
        };
    }

    static {
        specifications.add(syncTest("direct:syncInputAsyncOutput", "Simple send body to async output with valid response")
                .requestBody(xml("<baz/>"))
                .addExpectation(asyncExpectation("seda:asyncTarget").expectedBody(xml("<baz/>")))
                .expectedResponseBody(xml("<foo/>"))
                .build());

        specifications.add(syncTest("direct:syncInputAsyncOutput", "Ensure unresolved message count is zero and still valid")
                .expectedResponseBody(xml("<foo/>"))
                .requestBody(xml("<baz/>"))
                .addExpectation(unreceivedExpectation("seda:nothingToSeeHere"))
                .build());

        specifications.add(syncTest("direct:syncMultiTestPublisher", "Multiple messages received by expectation")
                .expectedResponseBody(xml("<foo/>"))
                .requestBody(xml("<baz/>"))
                .addExpectation(asyncExpectation("seda:asyncTarget")
                        .expectedMessageCount(3)
                        .expectedBody(xml("<moo/>")))
                .build());

        specifications.add(syncTest("direct:syncInputNoCallouts", "Message with no expectations")
                .expectedResponseBody(xml("<abc/>"))
                .requestBody(xml("<foo/>"))
                .build());

        specifications.add(syncTest("direct:syncMultiTestPublisher", "Test total ordering response the same")
                .requestBody(xml("<baz/>"))
                .addExpectation(asyncExpectation("seda:asyncTarget")
                        .expectedMessageCount(3).ordering(MockExpectation.OrderingType.PARTIAL))
                .build());

        specifications.add(syncTest("direct:syncMultiTestPublisher", "Test endpoint ordering response the same")
                .requestBody(xml("<baz/>"))
                .addExpectation(asyncExpectation("seda:asyncTarget")
                        .expectedMessageCount(3).ordering(MockExpectation.OrderingType.PARTIAL))
                .build());

        specifications.add(syncTest("direct:syncMultiTestPublisher", "Test no ordering response the same")
                .requestBody(xml("<baz/>"))
                .addExpectation(asyncExpectation("seda:asyncTarget")
                        .expectedMessageCount(3).ordering(MockExpectation.OrderingType.PARTIAL).endpointNotOrdered())
                .build());

        specifications.add(syncTest("direct:syncInputAsyncOutput", "Test headers are handled appropriately")
                .requestBody(xml("<baz/>"))
                .requestHeaders(headers(headervalue("foo", "baz"), headervalue("abc", "def")))
                .addExpectation(asyncExpectation("seda:asyncTarget")
                        .expectedHeaders(headers(headervalue("abc", "def"), headervalue("foo", "baz"))))
                .build());

        specifications.add(syncTest("direct:syncInputSyncOutput", "Test sync response")
                .requestBody(xml("<baz/>"))
                .addExpectation(syncExpectation("seda:syncTarget")
                        .expectedBody(xml("<baz/>")).responseBody(xml("<foo/>")))
                .expectedResponseBody(xml("<foo/>")).build());


        //Expected: s1.total,s2.partial,s3.partial
        //Actual s1,s3,s2
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