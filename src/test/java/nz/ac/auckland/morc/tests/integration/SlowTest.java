package nz.ac.auckland.morc.tests.integration;

import nz.ac.auckland.morc.MorcTestBuilder;
import nz.ac.auckland.morc.specification.AsyncOrchestratedTestBuilder;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;

public class SlowTest extends MorcTestBuilder {

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("vm:asyncConsumer")
                        .delay(5000l)
                        .to("log:foo");

                from("direct:syncConsumer")
                        .delay(5000l)
                        .setBody(constant("baz"));

                from("direct:verySlowSyncConsumer")
                        .delay(15000l)
                        .setBody(constant("baz"));

                //to simulate the time to put messages on a JMS destination
                from("direct:verySlowAsyncConsumer")
                        .delay(15000l)
                        .to("vm:asyncConsumer");
            }
        };
    }

    @Override
    public void configure() {
        /*
            A quick test to see what happens when we have a publisher that takes awhile to publish
            (we currently assume publishing time is close to 0)
         */
        AsyncOrchestratedTestBuilder slowProducerAsyncTestBuilder = asyncTest("Slow Producer Async Publisher Test", "vm:asyncConsumer");

        for (int i = 0; i < 10; i++) {
            slowProducerAsyncTestBuilder.addProcessors(new Processor() {
                @Override
                public void process(Exchange exchange) throws Exception {
                    Thread.sleep(5000l);
                    exchange.getIn().setBody("foo");
                }
            });
        }

        /*
            Another test illustrating what happens with long send interval and many messages that are slowly
            consumed, specifying a sensible result wait time
         */
        syncTest("Slow Consumer Test", "direct:syncConsumer")
                .messageResultWaitTime(500)
                .requestBody(times(10, text("foo")))
                //.sendInterval(5000l)
                .expectedResponse(times(10, text("baz")));

        syncTest("Very Slow Consumer Test with limited timeframes","direct:verySlowSyncConsumer")
                .requestBody(text("foo"))
                .expectedResponse(text("baz"));


        asyncTest("Very Slow Async Consumer Test with limited timeframes","direct:verySlowAsyncConsumer")
                .inputMessage(text("foo"));
    }

}
