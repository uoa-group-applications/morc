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

        syncTest("Simple WS proxy test", "http://localhost:8090/testWS")
                .request(xml(classpath("/data/pingRequest1.xml")))
                .expectation(xml(classpath("/data/pingResponse1.xml")), contentType("application/xml"))
                .addMock(syncMock("http://localhost:8090/targetWS")
                        .expectation(xml(classpath("/data/pingRequest1.xml")), contentType("application/xml"))
                        .response(xml(classpath("/data/pingResponse1.xml")))
                        .ordering(partialOrdering()));

        syncTest("Simple non-200 WS proxy test", "jetty:http://localhost:8090/testWS")
                .request(xml(classpath("/data/pingRequest1.xml")))
                .expectation(httpStatusCode(201), contentType("application/xml"))
                .addMock(syncMock("jetty:http://localhost:8090/targetWS")
                        .expectation(xml(classpath("/data/pingRequest1.xml")))
                        .response(httpStatusCode(201), xml("<foo/>")));

        syncTest("Simple non-200 WS proxy test check body", "jetty:http://localhost:8090/testWS")
                .request(xml(classpath("/data/pingRequest1.xml")))
                .expectation(httpStatusCode(201), xml("<foo/>"))
                .addMock(syncMock("jetty:http://localhost:8090/targetWS")
                        .expectation(xml(classpath("/data/pingRequest1.xml")))
                        .response(httpStatusCode(201), xml("<foo/>")));

        syncTest("Simple WS proxy failure test", "http://localhost:8090/testWS")
                .request(xml(classpath("/data/pingRequest1.xml")))
                .expectation(httpStatusCode(500))
                .expectsException()
                .addMock(syncMock("http://localhost:8090/targetWS")
                        .expectation(xml(classpath("/data/pingRequest1.xml")))
                        .response(httpStatusCode(500), xml(classpath("/data/pingSoapFault.xml"))));

        syncTest("Simple WS proxy failure test with body", "http://localhost:8090/testWS")
                .request(xml(classpath("/data/pingRequest1.xml")))
                .expectation(httpStatusCode(501), xml(classpath("/data/pingSoapFault.xml")))
                .expectsException()
                .addMock(syncMock("http://localhost:8090/targetWS")
                        .expectation(xml(classpath("/data/pingRequest1.xml")))
                        .response(httpStatusCode(501), xml(classpath("/data/pingSoapFault.xml"))));

        syncTest("Simple WS test using CXF", "cxf:http://localhost:8091/targetWS")
                .request(xml(classpath("/data/pingRequestCxf1.xml")))
                .expectation(xml(classpath("/data/pingResponseCxf1.xml")))
                .addMock(syncMock("cxf:http://localhost:8091/targetWS?wsdlURL=data/PingService.wsdl")
                        .expectation(xml(classpath("/data/pingRequestCxf1.xml")))
                        .response(xml(classpath("/data/pingResponseCxf1.xml"))));

        //Testing work around for https://issues.apache.org/jira/browse/CXF-2775
        syncTest("Duplicated WS test using CXF for CXF-2775 Work around", "cxf:http://localhost:8091/targetWS")
                .request(xml(classpath("/data/pingRequestCxf1.xml")))
                .expectation(xml(classpath("/data/pingResponseCxf1.xml")))
                .addMock(syncMock("cxf:http://localhost:8091/targetWS?wsdlURL=data/PingService.wsdl")
                        .expectation(xml(classpath("/data/pingRequestCxf1.xml")))
                        .response(xml(classpath("/data/pingResponseCxf1.xml"))));

        syncTest("CXF WS Fault Test", "cxf:http://localhost:8092/testWSFault")
                .request(xml(classpath("/data/pingRequestCxf1.xml")))
                .expectsException()
                .expectation(soapFault(soapFaultClient(), "Pretend SOAP Fault"));

        syncTest("CXF WS Fault Test with detail", "cxf:http://localhost:8092/testWSFaultDetail")
                .request(xml(classpath("/data/pingRequestCxf1.xml")))
                .expectsException()
                .expectation(soapFault(soapFaultServer(), "Pretend Detailed SOAP Fault",
                        xml("<detail><foo/></detail>")));

        syncTest("Simple test to show SOAP Fault expectation", "cxf:http://localhost:8092/targetWS")
                .request(xml(classpath("/data/pingRequestCxf1.xml")))
                .expectsException()
                .expectation(soapFault(soapFaultServer(), "Pretend Fault",
                        xml("<detail><foo/></detail>")))
                .addMock(syncMock("cxf:http://localhost:8092/targetWS?wsdlURL=data/PingService.wsdl")
                        .expectedMessageCount(1)
                        .response(soapFault(soapFaultServer(), "Pretend Fault", xml("<detail><foo/></detail>"))));

        syncTest("Simple WS proxy test", "http://localhost:8090/testWS")
                .request(xml(classpath("/data/pingRequest1.xml")))
                .expectation(xml(classpath("/data/pingResponse1.xml")))
                .addMock(syncMock("http://localhost:8090/targetWS")
                        .expectation(xml(classpath("/data/pingRequest1.xml")))
                        .response(xml(classpath("/data/pingResponse1.xml")))
                        .ordering(partialOrdering()))
                .addEndpoint("http://localhost:8090/testWS")
                .request(xml(classpath("/data/pingRequest1.xml")))
                .expectation(xml(classpath("/data/pingResponse1.xml")))
                .addMock(syncMock("http://localhost:8090/targetWS")
                        .expectation(xml(classpath("/data/pingRequest1.xml")))
                        .response(xml(classpath("/data/pingResponse1.xml")))
                        .ordering(partialOrdering()));

        syncTest("Simple multiple request WS proxy test", "jetty:http://localhost:8090/testWS")
                .request(xml(classpath("/data/pingRequest1.xml")))
                .request(xml(classpath("/data/pingRequest1.xml")))
                .request(xml(classpath("/data/pingRequest1.xml")))
                .expectationMultiplier(3, xml(classpath("/data/pingResponse1.xml")))
                .addMock(syncMock("jetty:http://localhost:8090/targetWS")
                        .expectationMultiplier(3, xml(classpath("/data/pingRequest1.xml")))
                        .responseMultiplier(3, xml(classpath("/data/pingResponse1.xml")))
                        .ordering(partialOrdering()));

        syncTest("Simple JSON PING", "http://localhost:8093/jsonPingService")
                .request(json("{\"request\":\"PING\"}"))
                .expectation(json("{\"response\":\"PONG\"}"));

        syncTest("Simple JSON PING no message", "http://localhost:8093/jsonPingService")
                .expectation(json("{\"response\":\"PONG\"}"));

        syncTest("Simple XML Groovy Test", "jetty:http://localhost:8090/testWS")
                .request(xml(groovy("<foo>$baz</foo>", var("baz", "123"))))
                .expectation(xml(groovy("<baz>$foo</baz>", var("foo", "321"))))
                .addMock(syncMock("jetty:http://localhost:8090/targetWS")
                        .expectation(xml(groovy("<foo>$x</foo>", var("x", "123"))))
                        .response(xml(groovy("<baz>$y</baz>", var("y", "321")))));

        syncTest("Simple HTTP Fail Test", "jetty:http://localhost:8090/testWS")
                .request(text("test"), httpMethod(POST()))
                .expectation(httpStatusCode(501), text("fail"))
                .expectsException()
                .addMock(syncMock("jetty:http://localhost:8090/targetWS")
                        .expectation(text("test"), httpMethod(POST()))
                        .response(text("fail"), httpStatusCode(501)));

        syncTest("Simple HTTP REST Test", "http://localhost:8094/testWS")
                .request(httpPath("/foo"), text("1"), httpMethod(POST())).expectation(httpStatusCode(203), text("2"))
                .request(httpPath("/baz"), text("3"), httpMethod(PUT())).expectation(httpStatusCode(203), text("4"))
                .request(httpPath("/moo"), text("5"), httpMethod(DELETE())).expectation(httpStatusCode(203), text("6"))
                .request(httpPath("/abc"), headers(header("foo", "baz")), httpMethod(GET())).expectation(httpStatusCode(203), text("7"))
                .addMock(restMock("http://localhost:8094/testWS")
                        .expectation(httpPath("/foo"), text("1"), httpMethod(POST())).response(text("2"), httpStatusCode(203))
                        .expectation(httpPath("/baz"), text("3"), httpMethod(PUT())).response(text("4"), httpStatusCode(203))
                        .expectation(httpPath("/moo"), text("5"), httpMethod(DELETE())).response(text("6"), httpStatusCode(203))
                        .expectation(httpPath("/abc"), headers(header("foo", "baz")), httpMethod(GET())).response(text("7"), httpStatusCode(203)));

        //without any path on the uri
        syncTest("Simple HTTP REST Test", "http://localhost:8094")
                .request(httpPath("/foo"), text("1"), httpMethod(POST())).expectation(httpStatusCode(203), text("2"))
                .request(httpPath("/baz"), text("3"), httpMethod(PUT())).expectation(httpStatusCode(203), text("4"))
                .request(httpPath("/moo"), text("5"), httpMethod(DELETE())).expectation(httpStatusCode(203), text("6"))
                .request(httpPath("/abc"), headers(header("foo", "baz")), httpMethod(GET())).expectation(httpStatusCode(203), text("7"))
                .addMock(restMock("http://localhost:8094")
                        .expectation(httpPath("/foo"), text("1"), httpMethod(POST())).response(text("2"), httpStatusCode(203))
                        .expectation(httpPath("/baz"), text("3"), httpMethod(PUT())).response(text("4"), httpStatusCode(203))
                        .expectation(httpPath("/moo"), text("5"), httpMethod(DELETE())).response(text("6"), httpStatusCode(203))
                        .expectation(httpPath("/abc"), headers(header("foo", "baz")), httpMethod(GET())).response(text("7"), httpStatusCode(203)));
    }

}
