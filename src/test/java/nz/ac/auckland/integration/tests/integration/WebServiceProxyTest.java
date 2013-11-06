package nz.ac.auckland.integration.tests.integration;

import nz.ac.auckland.integration.testing.OrchestratedTestBuilder;
import nz.ac.auckland.integration.testing.expectation.MockExpectation;
import org.apache.camel.builder.RouteBuilder;
import org.apache.cxf.binding.soap.SoapFault;

public class WebServiceProxyTest extends OrchestratedTestBuilder {

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                //a straight through proxy
                from("jetty:http://localhost:8090/testWS")
                        .to("jetty:http://localhost:8090/targetWS?bridgeEndpoint=true&throwExceptionOnFailure=false");

                SoapFault fault = new SoapFault("Pretend SOAP Fault", SoapFault.FAULT_CODE_CLIENT);

                from("cxf:http://localhost:8092/testWSFault?wsdlURL=data/PingService.wsdl&dataFormat=PAYLOAD")
                        .setFaultBody(constant(fault));

            }
        };
    }

    @Override
    public void configure() {
        syncTest("jetty:http://localhost:8090/testWS", "Simple WS proxy test")
                .requestBody(xml(classpath("/data/pingRequest1.xml")))
                .expectedResponseBody(xml(classpath("/data/pingResponse1.xml")))
                .addExpectation(syncExpectation("jetty:http://localhost:8090/targetWS")
                        .expectedBody(xml(classpath("/data/pingRequest1.xml")))
                        .responseBody(xml(classpath("/data/pingResponse1.xml")))
                        .ordering(MockExpectation.OrderingType.PARTIAL));

        syncTest("jetty:http://localhost:8090/testWS", "Simple WS proxy failure test")
                .requestBody(xml(classpath("/data/pingRequest1.xml")))
                .exceptionResponseValidator(httpException(500))
                .addExpectation(wsFaultExpectation("jetty:http://localhost:8090/targetWS")
                        .expectedBody(xml(classpath("/data/pingRequest1.xml")))
                        .responseBody(xml(classpath("/data/pingSoapFault.xml"))));

        syncTest("cxf:http://localhost:8091/targetWS", "Simple WS test using CXF")
                .requestBody(xml(classpath("/data/pingRequestCxf1.xml")))
                .expectedResponseBody(xml(classpath("/data/pingResponseCxf1.xml")))
                .addExpectation(syncExpectation("cxf:http://localhost:8091/targetWS?wsdlURL=data/PingService.wsdl")
                        .expectedBody(xml(classpath("/data/pingRequestCxf1.xml")))
                        .responseBody(xml(classpath("/data/pingResponseCxf1.xml"))));

        //Testing work around for https://issues.apache.org/jira/browse/CXF-2775
        syncTest("cxf:http://localhost:8091/targetWS", "Duplicated WS test using CXF for CXF-2775 Work around")
                        .requestBody(xml(classpath("/data/pingRequestCxf1.xml")))
                        .expectedResponseBody(xml(classpath("/data/pingResponseCxf1.xml")))
                        .addExpectation(syncExpectation("cxf:http://localhost:8091/targetWS?wsdlURL=data/PingService.wsdl")
                                .expectedBody(xml(classpath("/data/pingRequestCxf1.xml")))
                                .responseBody(xml(classpath("/data/pingResponseCxf1.xml"))));

        syncTest("cxf:http://localhost:8092/testWSFault","CXF WS Fault Test")
                        .requestBody(xml(classpath("/data/pingRequestCxf1.xml")))
                        .expectsExceptionResponse();
    }

}
