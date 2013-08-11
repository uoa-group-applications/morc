package nz.ac.auckland.integration.tests.integration;

import nz.ac.auckland.integration.testing.OrchestratedTest;
import nz.ac.auckland.integration.testing.expectation.MockExpectation;
import nz.ac.auckland.integration.testing.specification.OrchestratedTestSpecification;
import org.apache.camel.builder.RouteBuilder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.List;

import static nz.ac.auckland.integration.testing.dsl.SpecificationBuilderHelper.*;

@RunWith(value = Parameterized.class)
public class WebServiceProxyTest extends OrchestratedTest {

    protected static List<OrchestratedTestSpecification> specifications = new ArrayList<>();

    public WebServiceProxyTest(String[] springContextPaths, OrchestratedTestSpecification specification, String testName) {
        super(springContextPaths, specification);
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                //a straight through proxy
                from("jetty:http://localhost:8090/testWS")
                        .to("jetty:http://localhost:8090/targetWS?bridgeEndpoint=true&throwExceptionOnFailure=false");

            }
        };
    }

    static {
        specifications.add(syncTest("jetty:http://localhost:8090/testWS", "Simple WS proxy test")
                .requestBody(xml(classpath("/data/pingRequest1.xml")))
                .expectedResponseBody(xml(classpath("/data/pingResponse1.xml")))
                .addExpectation(syncExpectation("jetty:http://localhost:8090/targetWS")
                        .expectedBody(xml(classpath("/data/pingRequest1.xml")))
                        .responseBody(xml(classpath("/data/pingResponse1.xml"))).ordering(MockExpectation.OrderingType.PARTIAL))
                .build());

        specifications.add(syncTest("jetty:http://localhost:8090/testWS", "Simple WS proxy failure test")
                .requestBody(xml(classpath("/data/pingRequest1.xml")))
                .expectedResponseBody(xml(classpath("/data/pingSoapFault.xml")))
                .expectsExceptionResponse().exceptionValidator(httpExceptionResponseValidator())
                .addExpectation(wsFaultExpectation("jetty:http://localhost:8090/targetWS")
                        .expectedBody(xml(classpath("/data/pingRequest1.xml")))
                        .responseBody(xml(classpath("/data/pingSoapFault.xml"))))
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
