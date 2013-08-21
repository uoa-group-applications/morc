package com.acme.integration.tests.fakeesb;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.apache.camel.spring.SpringCamelContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * A fake ESB created with Camel to demonstrate how the test cases function
 * - in reality you wouldn't need this as your tests would be pointing
 * to your artifacts on the mega vendor's integration stack.
 */
public class FakeESB extends RouteBuilder {

    DefaultCamelContext context;

    public FakeESB() throws Exception {
        context = new SpringCamelContext(new ClassPathXmlApplicationContext("FakeESBContext.xml"));
        context.setName("FakeESB");
        context.addRoutes(this);
    }

    public void start() throws Exception {
        context.start();
    }

    public void stop() throws Exception {
        context.stop();
    }

    @Override
    public void configure() {
        from("direct:pingServiceResponse")
                .setBody(constant("<ns:pingResponse xmlns:ns=\"urn:com:acme:integration:wsdl:pingservice\">" +
                        "<response>PONG</response>" +
                        "</ns:pingResponse>"));

        from("cxf:bean:pingService")
            .to("direct:pingServiceResponse");

        from("cxf:bean:securePingService")
            .to("direct:pingServiceResponse");

        from("cxf:bean:pingServiceProxy")
                .to("direct:pingServiceProxy");

        from("direct:pingServiceProxy")
            //do some quick validation to show what happens on error
            .process(new Processor() {
                @Override
                public void process(Exchange exchange) throws Exception {
                    //a poor man's attempt at validation
                    if (!exchange.getIn().getBody(String.class).contains("PING"))
                        throw new Exception("INVALID BODY");
                }
            })
            //send this through to the 'target' system
            .to("cxf:http://localhost:9090/services/targetWS?dataFormat=PAYLOAD&wsdlURL=PingService.wsdl");

        from("cxf:bean:pingServiceMultiProxy")
                .multicast(new AggregationStrategy() {
                    @Override
                    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
                        return newExchange;
                    }
                }).stopOnException()
                .to("cxf:http://localhost:9090/services/targetWS?dataFormat=PAYLOAD&wsdlURL=PingService.wsdl",
                    "cxf:http://localhost:9091/services/anotherTargetWS?dataFormat=PAYLOAD&wsdlURL=PingService.wsdl");

        //parallel processing will mean this can happen in any order
        from("cxf:bean:pingServiceMultiProxyUnordered")
            .multicast(new AggregationStrategy() {
                @Override
                public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
                    return newExchange;
                }
            })
            .parallelProcessing()
            .to("direct:targetWSSlowDown",
            "cxf:http://localhost:9091/services/anotherTargetWS?dataFormat=PAYLOAD&wsdlURL=PingService.wsdl");

        //ensure they arrive out of order by delaying the first one
        from("direct:targetWSSlowDown")
                .delay(5000)
                .to("cxf:http://localhost:9090/services/targetWS?dataFormat=PAYLOAD&wsdlURL=PingService.wsdl");

        //The JSON Service is the easiest :)
        from("jetty:http://localhost:8091/jsonPingService")
            .setBody(constant("{\"response\":\"PONG\"}"));

        //Simple canonicalization service - the vm transport is like a JMS queue
        from("vm:test.input")
                .setBody(constant("<CanonicalField>foo</CanonicalField>"))
                .to("vm:test.output");

    }
}
