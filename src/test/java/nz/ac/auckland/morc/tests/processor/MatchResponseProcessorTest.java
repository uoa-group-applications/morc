package nz.ac.auckland.morc.tests.processor;

import nz.ac.auckland.morc.processor.MatchedResponseProcessor;
import nz.ac.auckland.morc.resource.PlainTextTestResource;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Assert;
import org.junit.Test;

public class MatchResponseProcessorTest extends Assert {

    @Test
    public void testMatchedResponse() throws Exception {

        MatchedResponseProcessor.MatchedResponse response = new MatchedResponseProcessor.MatchedResponse(
                new PlainTextTestResource("foo"), new PlainTextTestResource("baz"));
        MatchedResponseProcessor.MatchedResponse response1 = new MatchedResponseProcessor.MatchedResponse(
                new PlainTextTestResource("baz"), new PlainTextTestResource("foo"));

        MatchedResponseProcessor processor = new MatchedResponseProcessor(response, response1);

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.getIn().setBody("foo");
        processor.process(e);
        assertEquals("baz", e.getIn().getBody(String.class));

        Exchange e1 = new DefaultExchange(new DefaultCamelContext());
        e1.getIn().setBody("baz");
        processor.process(e1);

        assertEquals("foo", e1.getIn().getBody(String.class));

        Exchange e2 = new DefaultExchange(new DefaultCamelContext());
        e2.getIn().setBody("moo");
        processor.process(e2);

        //nothing changed in exchange (as expected)
        assertEquals("moo", e2.getIn().getBody(String.class));
    }

    @Test
    public void testDefaultProcessor() throws Exception {
        MatchedResponseProcessor.MatchedResponse response = new MatchedResponseProcessor.MatchedResponse(
                new PlainTextTestResource("foo"), new PlainTextTestResource("baz"));
        MatchedResponseProcessor.MatchedResponse response1 = new MatchedResponseProcessor.MatchedResponse(
                new PlainTextTestResource("baz"), new PlainTextTestResource("foo"));

        MatchedResponseProcessor.DefaultMatchedResponse defaultResponse = new MatchedResponseProcessor.DefaultMatchedResponse(
                new PlainTextTestResource("moo"));

        MatchedResponseProcessor processor = new MatchedResponseProcessor(defaultResponse, response, response1);


        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.getIn().setBody("foo");
        processor.process(e);
        assertEquals("baz", e.getIn().getBody(String.class));

        Exchange e1 = new DefaultExchange(new DefaultCamelContext());
        e1.getIn().setBody("baz");
        processor.process(e1);

        assertEquals("foo", e1.getIn().getBody(String.class));

        Exchange e2 = new DefaultExchange(new DefaultCamelContext());
        e2.getIn().setBody("123");
        processor.process(e2);
        assertEquals("moo", e2.getIn().getBody(String.class));

        Exchange e3 = new DefaultExchange(new DefaultCamelContext());
        e3.getIn().setBody("foo");
        processor.process(e3);
        assertEquals("baz", e3.getIn().getBody(String.class));

        Exchange e4 = new DefaultExchange(new DefaultCamelContext());
        e4.getIn().setBody("456");
        processor.process(e4);
        assertEquals("moo", e4.getIn().getBody(String.class));
    }


}
