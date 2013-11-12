package nz.ac.auckland.integration.tests.integration;

import nz.ac.auckland.integration.testing.OrchestratedTestBuilder;
import nz.ac.auckland.integration.testing.utility.XmlUtilities;
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

                SoapFault detailedFault = new SoapFault("Pretend Detailed SOAP Fault", SoapFault.FAULT_CODE_SERVER);
                detailedFault.setDetail(new XmlUtilities().getXmlAsDocument("<detail><foo/></detail>").getDocumentElement());

                from("cxf:http://localhost:8092/testWSFaultDetail?wsdlURL=data/PingService.wsdl&dataFormat=PAYLOAD")
                        .setFaultBody(constant(detailedFault));
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
                        .ordering(partialOrdering()));

        syncTest("jetty:http://localhost:8090/testWS", "Simple WS proxy failure test")
                .requestBody(xml(classpath("/data/pingRequest1.xml")))
                .exceptionResponseValidator(httpExceptionResponse(500))
                .addExpectation(httpErrorExpectation("jetty:http://localhost:8090/targetWS")
                        .expectedBody(xml(classpath("/data/pingRequest1.xml")))
                        .responseBody(xml(classpath("/data/pingSoapFault.xml"))));

        syncTest("jetty:http://localhost:8090/testWS", "Simple WS proxy failure test with body")
                .requestBody(xml(classpath("/data/pingRequest1.xml")))
                .exceptionResponseValidator(httpExceptionResponse(501,xml(classpath("/data/pingSoapFault.xml"))))
                .addExpectation(httpErrorExpectation("jetty:http://localhost:8090/targetWS")
                        .expectedBody(xml(classpath("/data/pingRequest1.xml")))
                        .responseBody(xml(classpath("/data/pingSoapFault.xml")))
                        .statusCode(501));

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

        syncTest("cxf:http://localhost:8092/testWSFault", "CXF WS Fault Test")
                .requestBody(xml(classpath("/data/pingRequestCxf1.xml")))
                .exceptionResponseValidator(soapFaultResponse(SOAPFAULT_CLIENT, "Pretend SOAP Fault"));

        syncTest("cxf:http://localhost:8092/testWSFaultDetail", "CXF WS Fault Test with detail")
                .requestBody(xml(classpath("/data/pingRequestCxf1.xml")))
                .exceptionResponseValidator(soapFaultResponse(SOAPFAULT_SERVER, "Pretend Detailed SOAP Fault",
                        xml("<detail><foo/></detail>")));

        syncTest("cxf:http://localhost:8092/targetWS", "Simple test to show SOAP Fault expectation")
                .requestBody(xml(classpath("/data/pingRequestCxf1.xml")))
                .exceptionResponseValidator(soapFaultResponse(SOAPFAULT_SERVER, "Pretend Fault",
                        xml("<detail><foo/></detail>")))
                .addExpectation(soapFaultExpectation("cxf:http://localhost:8092/targetWS?wsdlURL=data/PingService.wsdl")
                        .responseBody(soapFault(SOAPFAULT_SERVER, "Pretend Fault", xml("<detail><foo/></detail>"))));


    }

}
