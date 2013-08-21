package com.acme.integration.tests;

import com.acme.integration.tests.fakeesb.FakeESB;
import nz.ac.auckland.integration.testing.OrchestratedTest;
import nz.ac.auckland.integration.testing.expectation.MockExpectation;
import nz.ac.auckland.integration.testing.specification.OrchestratedTestSpecification;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.test.annotation.DirtiesContext;

import java.util.ArrayList;
import java.util.List;

import static nz.ac.auckland.integration.testing.dsl.SpecificationBuilderHelper.*;

/**
 * A JUNIT Parameterized Test that executes each test specification as a separate test
 * - this bootstrap code is expected to be reduced further in upcoming releases
 */
@RunWith(value = Parameterized.class)
public class AcmeTest extends OrchestratedTest {
    protected static List<OrchestratedTestSpecification> specifications = new ArrayList<>();

    private FakeESB esb;

    public AcmeTest(OrchestratedTestSpecification specification, String testName) throws Exception {
        super(specification);

        //This is to set up a magical ESB (you won't normally need to do this)
        esb = new FakeESB();
    }

    static {
        //Sends a request to pingService and validates the response
        specifications.add(syncTest("cxf:http://localhost:8090/services/pingService","Simple WS PING test")
                .requestBody(xml("<ns:pingRequest xmlns:ns=\"urn:com:acme:integration:wsdl:pingservice\">" +
                                    "<request>PING</request>" +
                                 "</ns:pingRequest>"))
                .expectedResponseBody(xml("<ns:pingResponse xmlns:ns=\"urn:com:acme:integration:wsdl:pingservice\">" +
                        "<response>PONG</response>" +
                        "</ns:pingResponse>"))
                .build());

        //Using WS-Security features of CXF (username/password) - note you need to specify the WSDL so
        //that CXF can grab the policy (it can be a remote reference if you wish)
        specifications.add(syncTest("cxf://http://localhost:8090/services/securePingService?wsdlURL=SecurePingService.wsdl&" +
                "properties.ws-security.username=user" +
                "&properties.ws-security.password=pass",
                "Simple WS PING test with WS-Security")
                .requestBody(xml("<ns:pingRequest xmlns:ns=\"urn:com:acme:integration:wsdl:pingservice\">" +
                                    "<request>PING</request>" +
                                 "</ns:pingRequest>"))
                .expectedResponseBody(xml("<ns:pingResponse xmlns:ns=\"urn:com:acme:integration:wsdl:pingservice\">" +
                        "<response>PONG</response>" +
                        "</ns:pingResponse>"))
                .build());

        //Using classpath resources instead
        specifications.add(syncTest("cxf:http://localhost:8090/services/pingService",
                "Simple WS PING test with local resources")
                .requestBody(xml(classpath("/data/pingRequest1.xml")))
                .expectedResponseBody(xml(classpath("/data/pingResponse1.xml")))
                .build());

        //Using a JSON service
        specifications.add(syncTest("http://localhost:8091/jsonPingService", "Simple JSON PING")
                .requestBody(json("{\"request\":\"PING\"}"))
                .expectedResponseBody(json("{\"response\":\"PONG\"}"))
                .build());

        //Showing how expectations can create mock endpoints to validate the incoming request and provide a canned response
        specifications.add(syncTest("cxf:http://localhost:8090/services/pingServiceProxy",
                "WS PING test with mock service expectation")
                .requestBody(xml(classpath("/data/pingRequest1.xml")))
                .expectedResponseBody(xml(classpath("/data/pingResponse1.xml")))
                .addExpectation(syncExpectation("cxf:http://localhost:9090/services/targetWS?wsdlURL=PingService.wsdl")
                        .expectedBody(xml(classpath("/data/pingRequest1.xml")))
                        .responseBody(xml(classpath("/data/pingResponse1.xml"))))
                .build());

        //Showing how we can string together expectations for multiple requests to the same (or different) endpoints
        specifications.add(syncTest("cxf:http://localhost:8090/services/pingServiceMultiProxy",
                "WS PING test with multiple mock service expectations")
                .requestBody(xml(classpath("/data/pingRequest1.xml")))
                .expectedResponseBody(xml(classpath("/data/pingResponse1.xml")))
                .addExpectation(syncExpectation("cxf:http://localhost:9090/services/targetWS?wsdlURL=PingService" +
                        ".wsdl")
                        .expectedBody(xml(classpath("/data/pingRequest1.xml")))
                        .responseBody(xml(classpath("/data/pingResponse1.xml"))))
                .addExpectation(syncExpectation
                        ("cxf:http://localhost:9091/services/anotherTargetWS?wsdlURL=PingService.wsdl")
                        .expectedBody(xml(classpath("/data/pingRequest1.xml")))
                        .responseBody(xml(classpath("/data/pingResponse1.xml"))))
                .build());


        //The same as above except showing support for weakly ordered expectations (i.e. multi-threaded call-outs)
        specifications.add(syncTest("cxf:http://localhost:8090/services/pingServiceMultiProxyUnordered",
                "WS PING test with multiple unordered mock service expectations")
                .requestBody(xml(classpath("/data/pingRequest1.xml")))
                .expectedResponseBody(xml(classpath("/data/pingResponse1.xml")))
                .addExpectation(syncExpectation("cxf:http://localhost:9090/services/targetWS?wsdlURL=PingService.wsdl")
                        .expectedBody(xml(classpath("/data/pingRequest1.xml")))
                        .responseBody(xml(classpath("/data/pingResponse1.xml")))
                        .ordering(MockExpectation.OrderingType.PARTIAL))
                .addExpectation(syncExpectation("cxf:http://localhost:9091/services/anotherTargetWS?wsdlURL=PingService.wsdl")
                        .expectedBody(xml(classpath("/data/pingRequest1.xml")))
                        .responseBody(xml(classpath("/data/pingResponse1.xml")))
                        .ordering(MockExpectation.OrderingType.PARTIAL))
                .build());

        /*
        Send an invalid message to the ESB which validates and rejects it,  meaning the target endpoint shouldn't
         receive it. The unreceivedExpectation is especially useful for message filtering where you want to
         ensure the message doesn't arrive at the endpoint.
         */

        specifications.add(syncTest("cxf:http://localhost:8090/services/pingServiceProxy",
                "Test invalid message doesn't arrive at the endpoint and returns exception")
                .requestBody(xml("<ns:pingRequest xmlns:ns=\"urn:com:acme:integration:wsdl:pingservice\">" +
                                                    "<request>PONG</request>" +
                                                 "</ns:pingRequest>"))
                .expectsExceptionResponse()
                .addExpectation(unreceivedExpectation("cxf:http://localhost:9090/services/targetWS?wsdlURL=PingService.wsdl"))
                .build());

        //Send a message to a vm destination (like a JMS queue) to show asynchronous messaging with transformation
        specifications.add(asyncTest("vm:test.input", "Simple Asynchronous Canonicalizer Comparison")
                .inputMessage(xml("<SystemField>foo</SystemField>"))
                .addExpectation(asyncExpectation("vm:test.output")
                        .expectedBody(xml("<CanonicalField>foo</CanonicalField>")))
                .build());


    }

    //this is used by JUnit to initialize each instance of this specification
    @Parameterized.Parameters(name = "{index}: {1}")
    public static java.util.Collection<Object[]> data() {
        List<Object[]> constructorInputs = new ArrayList<>();

        for (OrchestratedTestSpecification spec : specifications) {
            Object[] constructorInput = new Object[]{spec, spec.getDescription()};
            constructorInputs.add(constructorInput);
        }

        return constructorInputs;
    }

    /*
    The wizard behind the curtains - sets up and tears down the integration processes for demonstration purposes
    */
    @Override
    public void setUp() throws Exception {
        super.setUp();
        esb.start();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        esb.stop();
    }

}
