package nz.ac.auckland.morc.tests.orchestrated;

import nz.ac.auckland.morc.MorcTestBuilder;
import nz.ac.auckland.morc.mock.MockDefinition;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;

import java.io.FileNotFoundException;
import java.io.IOException;


/**
 * Simple 1 expectation synchronous tests for sending and receiving messages using the Camel infrastructure
 */
public class MultiExpectationSyncTest extends MorcTestBuilder {

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:syncInput")
                        .to("seda:asyncTargetInternal?waitForTaskToComplete=Never")
                        .to("seda:syncTarget?waitForTaskToComplete=Always");

                from("seda:asyncTargetInternal")
                        .setBody(constant("<async/>"))
                        .delay(2500)
                        .to("seda:asyncTarget");

                from("seda:asyncTargetInternal1")
                        .setBody(constant("<async/>"))
                        .delay(5000)
                        .to("seda:asyncTarget1");

                from("direct:syncInputMultiAsync")
                        .to("seda:asyncTargetInternal?waitForTaskToComplete=Never")
                        .to("seda:asyncTargetInternal1?waitForTaskToComplete=Never")
                        .to("seda:syncTarget?waitForTaskToComplete=Always");

                from("direct:syncInputMultiAsyncToSameDest")
                        .to("seda:asyncTargetInternal?waitForTaskToComplete=Never")
                        .to("seda:asyncTargetInternal1?waitForTaskToComplete=Never")
                        .to("seda:asyncTargetInternal1?waitForTaskToComplete=Never")
                        .to("seda:syncTarget?waitForTaskToComplete=Always");

                from("direct:multiSend")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                template.sendBody("seda:syncMultiSendEndpoint0", exchange.getIn().getBody());
                                Thread.sleep(100);
                                template.sendBody("seda:syncMultiSendEndpoint2?waitForTaskToComplete=Never", exchange.getIn().getBody());
                                Thread.sleep(5000);
                                template.sendBody("seda:syncMultiSendEndpoint1?waitForTaskToComplete=Never", exchange.getIn().getBody());
                            }
                        });

                from("direct:multiSend1")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                template.sendBody("seda:syncMultiSendEndpoint0", exchange.getIn().getBody());
                                Thread.sleep(100);
                                template.sendBody("seda:syncMultiSendEndpoint1", "<first/>");
                                Thread.sleep(5000);
                                template.sendBody("seda:syncMultiSendEndpoint1", "<second/>");
                            }
                        });

                from("direct:multiSend2")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                template.sendBody("seda:syncMultiSendEndpoint0", exchange.getIn().getBody());
                                Thread.sleep(100);
                                template.sendBody("seda:syncMultiSendEndpoint2", "<first/>");
                                Thread.sleep(2000);
                                template.sendBody("seda:syncMultiSendEndpoint1", "<second/>");
                                Thread.sleep(2000);
                                template.sendBody("seda:syncMultiSendEndpoint2", "<third/>");
                                Thread.sleep(2000);
                                template.sendBody("seda:syncMultiSendEndpoint1", "<fourth/>");
                            }

                        });

                from("direct:syncAtEnd")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                template.sendBody("seda:b", "");
                                Thread.sleep(2000);
                                template.sendBody("seda:a", "1");
                                Thread.sleep(2000);
                                template.sendBody("seda:a", "2");
                            }

                        });

                from("direct:endpointUnorderedWithSyncInMiddle")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                template.sendBody("seda:a", "3");
                                Thread.sleep(1000);
                                template.sendBody("seda:a", "2");
                                Thread.sleep(1000);
                                template.sendBody("seda:s", "1");
                                Thread.sleep(1000);
                                template.sendBody("seda:a", "1");
                            }

                        });

                from("direct:endpointWithSyncOrdering")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                template.sendBody("seda:b", "");
                                Thread.sleep(1000);
                                template.sendBody("seda:s", "");
                                Thread.sleep(1000);
                                template.sendBody("seda:a", "");
                            }
                        });

                from("vm:asyncSyncAndEndpointNotOrderedDontMix")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                template.sendBody("vm:a", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
                                Thread.sleep(500);
                                template.sendBody("vm:a", "2");
                                Thread.sleep(500);
                                template.sendBody("vm:s", "");
                                Thread.sleep(500);
                                template.sendBody("vm:a", "1");
                            }
                        });
            }
        };
    }

    @Override
    public void configure() {
        syncTest("Simple send body to two destinations and get correct response", "direct:syncInput")
                .expectation(xml("<foo/>"))
                .request(xml("<baz/>"))
                .addMock(asyncMock("seda:asyncTarget").expectation(xml("<async/>")))
                .addMock(syncMock("seda:syncTarget").expectation(xml("<baz/>")).
                        response(xml("<foo/>")));

        syncTest("Simple send body to two destinations with swapped order", "direct:syncInput")
                .expectation(xml("<foo/>"))
                .request(xml("<baz/>"))
                .addMock(syncMock("seda:syncTarget")
                        .expectation(xml("<baz/>"))
                        .response(xml("<foo/>")))
                .addMock(asyncMock("seda:asyncTarget").expectation(xml("<async/>")));

        syncTest("Sync and multiple Async - ensuring total order", "direct:syncInputMultiAsync")
                .expectation(xml("<foo/>"))
                .request(xml("<baz/>"))
                        //this expectation will come in last
                .addMock(asyncMock("seda:asyncTarget1").expectation(xml("<async/>")))
                        //this expectation will come in first
                .addMock(syncMock("seda:syncTarget").expectation(xml("<baz/>")).
                        response(xml("<foo/>")))
                        //this expectation will come in second to last
                .addMock(asyncMock("seda:asyncTarget").expectation(xml("<async/>")));

        /*
            Note we expect to see an exception at the end of this as the last asyncMock("seda:asyncTarget1") has
            no expected messages and the delay will mean the message never arrives on time.
         */
        syncTest("Sync and multiple Async to same dest - ensuring total order", "direct:syncInputMultiAsyncToSameDest")
                .expectation(xml("<foo/>"))
                .request(xml("<baz/>"))
                .addMock(asyncMock("seda:asyncTarget1").expectation(xml("<async/>")))
                .addMock(syncMock("seda:syncTarget").expectation(xml("<baz/>")).
                        response(xml("<foo/>")))
                .addMock(asyncMock("seda:asyncTarget").expectation(xml("<async/>")))
                .addMock(asyncMock("seda:asyncTarget1"));

        syncTest("Send to two sync destinations without total ordering", "direct:multiSend")
                .request(xml("<foo/>"))
                .addMock(syncMock("seda:syncMultiSendEndpoint0").expectedMessageCount(1))
                        //this will receive the message last
                .addMock(syncMock("seda:syncMultiSendEndpoint1").expectedMessageCount(1)
                        .ordering(MockDefinition.OrderingType.PARTIAL))
                        //this will receive the message first
                .addMock(syncMock("seda:syncMultiSendEndpoint2").expectedMessageCount(1)
                        .ordering(MockDefinition.OrderingType.PARTIAL));

        syncTest("Send unordered messages to same sync endpoint without endpoint ordering", "direct:multiSend1")
                .request(xml("<foo/>"))
                .addMock(syncMock("seda:syncMultiSendEndpoint0").expectedMessageCount(1))
                        //we will receive this last
                .addMock(syncMock("seda:syncMultiSendEndpoint1").endpointNotOrdered()
                        .expectation(xml("<second/>")))
                        //we will receive this first
                .addMock(syncMock("seda:syncMultiSendEndpoint1").endpointNotOrdered()
                        .expectation(xml("<first/>")));

        syncTest("Send unordered messages to two different sync destinations without total ordering or endpoint ordering", "direct:multiSend2")
                .request(xml("<foo/>"))
                .addMock(syncMock("seda:syncMultiSendEndpoint0").expectedMessageCount(1))
                .addMock(syncMock("seda:syncMultiSendEndpoint1").endpointNotOrdered().ordering(partialOrdering())
                        .expectation(xml("<fourth/>")))
                .addMock(syncMock("seda:syncMultiSendEndpoint2").endpointNotOrdered().ordering(partialOrdering())
                        .expectation(xml("<third/>")))
                .addMock(syncMock("seda:syncMultiSendEndpoint1").endpointNotOrdered().ordering(partialOrdering())
                        .expectation(xml("<second/>")))
                .addMock(syncMock("seda:syncMultiSendEndpoint2").endpointNotOrdered().ordering(partialOrdering())
                        .expectation(xml("<first/>")));

        syncTest("Send async messages out of order such that sync arrives first", "direct:syncAtEnd")
                .request(text("0"))
                .addMock(asyncMock("seda:a").expectation(text("2")).endpointNotOrdered().expectedMessageCount(1))
                .addMock(asyncMock("seda:a").expectation(text("1")).endpointNotOrdered().expectedMessageCount(1))
                .addMock(syncMock(("seda:b")).expectedMessageCount(1));

        asyncTest("send mis-ordered", "direct:endpointWithSyncOrdering")
                .input(text("0"))
                .addMock(asyncMock("seda:a").expectedMessageCount(1))
                .addMock(asyncMock("seda:b").expectedMessageCount(1))
                .addMock(syncMock("seda:s").expectedMessageCount(1));

        syncTest("Test Lenient Processor", "seda:lenient?waitForTaskToComplete=Always")
                .request(text("1")).request(text("2")).request(text("3")).request(text("4"))
                .expectation(text("-1")).expectation(text("-2")).expectation(text("-3")).expectation(text("-1"))
                .addMock(syncMock("seda:lenient").lenient()
                        .response(text("-1")).response(text("-2")).response(text("-3")));

        syncTest("Match response Lenient Processor", "seda:lenient?waitForTaskToComplete=Always")
                .request(text("1"), headers(header("5", "5")))
                .request(text("2"), headers(header("6", "6")))
                .request(text("3"), headers(header("7", "7")))
                .request(text("4"), headers(header("8", "8")))
                .expectation(text("-1"), headers(header("-5", "-5")))
                .expectation(text("-2"), headers(header("-6", "-6")))
                .expectation(text("-3"), headers(header("-7", "-7")))
                .expectation(text("-4"), headers(header("-8", "-8")))
                .addMock(syncMock("seda:lenient").lenient()
                        .addProcessors(0, matchedResponse(answer(text("4"), text("-4"), headers(header("-8", "-8"))),
                                answer(text("3"), text("-3"), headers(header("-7", "-7")))
                                , answer(text("2"), text("-2"), headers(header("-6", "-6")))
                                , answer(text("1"), text("-1"), headers(header("-5", "-5"))))));

        syncTest("Test Partial Lenient Processor", "seda:partialLenient?waitForTaskToComplete=Always")
                .request(text("1")).request(text("2")).request(text("3")).request(text("4"))
                .expectation(text("-1")).expectation(text("-2")).expectation(text("-3")).expectation(text("-4"))
                .addMock(syncMock("seda:partialLenient").lenient(
                        exchange ->
                                Integer.parseInt(exchange.getIn().getBody(String.class)) % 2 == 0)
                        .response(text("-2")).response(text("-4")))
                .addMock(syncMock("seda:partialLenient")
                        .addRepeatedPredicate(exchange ->
                                exchange.getPattern().equals(ExchangePattern.InOut))
                        .expectation(text("1")).expectation(text("3"))
                        .response(text("-1")).response(text("-3")));

        syncTest("Test throw receive exceptions", "seda:throwsException?waitForTaskToComplete=Always")
                .requestMultiplier(5, text("1"))
                .expectation(exception())
                .expectation(exception(new IOException()))
                .expectation(exception(new IOException("foo")))
                .expectation(exception(new FileNotFoundException()))
                .expectation(exception(new FileNotFoundException("baz")))
                .expectsException()
                .addMock(syncMock("seda:throwsException").expectedMessageCount(1)
                        .response(exception()))
                .addMock(syncMock("seda:throwsException").expectedMessageCount(1)
                        .response(exception(new IOException())))
                .addMock(syncMock("seda:throwsException").expectedMessageCount(1)
                        .response(exception(new IOException("foo"))))
                .addMock(syncMock("seda:throwsException").response(exception(new FileNotFoundException()))
                        .expectedMessageCount(1))
                .addMock(syncMock("seda:throwsException").response(exception(new FileNotFoundException("baz")))
                        .expectedMessageCount(1));
    }

}