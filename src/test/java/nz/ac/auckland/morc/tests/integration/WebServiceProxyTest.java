package nz.ac.auckland.morc.tests.integration;

import nz.ac.auckland.morc.MorcTestBuilder;
import nz.ac.auckland.morc.utility.XmlUtilities;
import org.apache.camel.builder.RouteBuilder;
import org.apache.cxf.binding.soap.SoapFault;

public class WebServiceProxyTest extends MorcTestBuilder {

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

                SoapFault detailedFault = new SoapFault("Pretend Detailed SOAP Fault", SoapFault.FAULT_CODE_SERVER);
                detailedFault.setDetail(new XmlUtilities().getXmlAsDocument("<detail><foo/></detail>").getDocumentElement());

                from("cxf:http://localhost:8092/testWSFaultDetail?wsdlURL=data/PingService.wsdl&dataFormat=PAYLOAD")
                        .setFaultBody(constant(detailedFault));

                from("jetty:http://localhost:8093/jsonPingService")
                                  .setBody(constant("{\"response\":\"PONG\"}"));
            }
        };
    }

    @Override
    public void configure() {
        syncTest("Simple WS proxy test", "jetty:http://localhost:8090/testWS")
                .requestBody(xml(classpath("/data/pingRequest1.xml")))
                .expectedResponseBody(xml(classpath("/data/pingResponse1.xml")))
                .addExpectation(syncExpectation("jetty:http://localhost:8090/targetWS")
                        .expectedBody(xml(classpath("/data/pingRequest1.xml")))
                        .responseBody(xml(classpath("/data/pingResponse1.xml")))
                        .ordering(partialOrdering()));

        syncTest("Simple WS proxy failure test", "jetty:http://localhost:8090/testWS")
                .requestBody(xml(classpath("/data/pingRequest1.xml")))
                .expectedResponse(httpExceptionResponse(500))
                .expectsException()
                .addExpectation(httpErrorExpectation("jetty:http://localhost:8090/targetWS")
                        .expectedBody(xml(classpath("/data/pingRequest1.xml")))
                        .responseBody(xml(classpath("/data/pingSoapFault.xml"))));

        syncTest("Simple WS proxy failure test with body", "jetty:http://localhost:8090/testWS")
                .requestBody(xml(classpath("/data/pingRequest1.xml")))
                .expectedResponse(httpExceptionResponse(501, xml(classpath("/data/pingSoapFault.xml"))))
                .expectsException()
                .addExpectation(httpErrorExpectation("jetty:http://localhost:8090/targetWS")
                        .expectedBody(xml(classpath("/data/pingRequest1.xml")))
                        .responseBody(xml(classpath("/data/pingSoapFault.xml")))
                        .statusCode(501));

        syncTest("Simple WS test using CXF", "cxf:http://localhost:8091/targetWS")
                .requestBody(xml(classpath("/data/pingRequestCxf1.xml")))
                .expectedResponseBody(xml(classpath("/data/pingResponseCxf1.xml")))
                .addExpectation(syncExpectation("cxf:http://localhost:8091/targetWS?wsdlURL=data/PingService.wsdl")
                        .expectedBody(xml(classpath("/data/pingRequestCxf1.xml")))
                        .responseBody(xml(classpath("/data/pingResponseCxf1.xml"))));

        //Testing work around for https://issues.apache.org/jira/browse/CXF-2775
        syncTest("Duplicated WS test using CXF for CXF-2775 Work around", "cxf:http://localhost:8091/targetWS")
                .requestBody(xml(classpath("/data/pingRequestCxf1.xml")))
                .expectedResponseBody(xml(classpath("/data/pingResponseCxf1.xml")))
                .addExpectation(syncExpectation("cxf:http://localhost:8091/targetWS?wsdlURL=data/PingService.wsdl")
                        .expectedBody(xml(classpath("/data/pingRequestCxf1.xml")))
                        .responseBody(xml(classpath("/data/pingResponseCxf1.xml"))));

        syncTest("CXF WS Fault Test", "cxf:http://localhost:8092/testWSFault")
                .requestBody(xml(classpath("/data/pingRequestCxf1.xml")))
                .expectsException()
                .expectedResponse(soapFault(SOAPFAULT_CLIENT, "Pretend SOAP Fault"));

        syncTest("CXF WS Fault Test with detail", "cxf:http://localhost:8092/testWSFaultDetail")
                .requestBody(xml(classpath("/data/pingRequestCxf1.xml")))
                .expectsException()
                .expectedResponse(soapFault(SOAPFAULT_SERVER, "Pretend Detailed SOAP Fault",
                        xml("<detail><foo/></detail>")));

        syncTest("Simple test to show SOAP Fault expectation", "cxf:http://localhost:8092/targetWS")
                .requestBody(xml(classpath("/data/pingRequestCxf1.xml")))
                .expectsException()
                .expectedResponse(soapFault(SOAPFAULT_SERVER, "Pretend Fault",
                        xml("<detail><foo/></detail>")))
                .addExpectation(soapFaultExpectation("cxf:http://localhost:8092/targetWS?wsdlURL=data/PingService.wsdl")
                        .expectedMessageCount(1)
                        .responseBody(soapFault(SOAPFAULT_SERVER, "Pretend Fault", xml("<detail><foo/></detail>"))));

        syncTest("Simple WS proxy test", "jetty:http://localhost:8090/testWS")
                .requestBody(xml(classpath("/data/pingRequest1.xml")))
                .expectedResponseBody(xml(classpath("/data/pingResponse1.xml")))
                .addExpectation(syncExpectation("jetty:http://localhost:8090/targetWS")
                        .expectedBody(xml(classpath("/data/pingRequest1.xml")))
                        .responseBody(xml(classpath("/data/pingResponse1.xml")))
                        .ordering(partialOrdering()))
                .addEndpoint("jetty:http://localhost:8090/testWS")
                .requestBody(xml(classpath("/data/pingRequest1.xml")))
                .expectedResponseBody(xml(classpath("/data/pingResponse1.xml")))
                .addExpectation(syncExpectation("jetty:http://localhost:8090/targetWS")
                        .expectedBody(xml(classpath("/data/pingRequest1.xml")))
                        .responseBody(xml(classpath("/data/pingResponse1.xml")))
                        .ordering(partialOrdering()));

        syncTest("Simple multiple request WS proxy test", "jetty:http://localhost:8090/testWS")
                .requestBody(xml(classpath("/data/pingRequest1.xml")), xml(classpath("/data/pingRequest1.xml")), xml(classpath("/data/pingRequest1.xml")))
                .expectedResponseBody(times(3, xml(classpath("/data/pingResponse1.xml"))))
                .addExpectation(syncExpectation("jetty:http://localhost:8090/targetWS")
                        .expectedBody(times(3, xml(classpath("/data/pingRequest1.xml"))))
                        .responseBody(times(3, xml(classpath("/data/pingResponse1.xml"))))
                        .ordering(partialOrdering()));

        syncTest("Simple JSON PING","http://localhost:8093/jsonPingService")
                        .requestBody(json("{\"request\":\"PING\"}"))
                        .expectedResponseBody(json("{\"response\":\"PONG\"}"));
    }

}
