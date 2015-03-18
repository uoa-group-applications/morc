package nz.ac.auckland.morc.tests.integration;

import nz.ac.auckland.morc.MorcTestBuilder;
import org.apache.camel.builder.RouteBuilder;

//A quick test to ensure large messages are streamed correctly
public class LargeMessageProxyTest extends MorcTestBuilder {
    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                //a straight through proxy
                from("cxf:bean:pingService")
                        .to("cxf:http://localhost:9090/pingService?wsdlURL=data/PingService.wsdl&dataFormat=PAYLOAD");
            }
        };
    }

    @Override
    public String[] getSpringContextPaths() {
        return new String[]{"large-message-context.xml"};
    }

    @Override
    public void configure() {
        syncTest("Large Message Proxy Test", "cxf:http://localhost:9091/pingService")
                .request(xml(classpath("/data/bigPingRequest.xml")))
                .expectation(xml(classpath("/data/bigPingResponse.xml")))
                .addExpectation(syncExpectation("cxf:http://localhost:9090/pingService?wsdlURL=data/PingService.wsdl")
                        .expectation(xml(classpath("/data/bigPingRequest.xml")))
                        .response(xml(classpath("/data/bigPingResponse.xml"))));
    }
}
