package nz.ac.auckland.integration.tests.processor;

import nz.ac.auckland.integration.testing.processor.BodyProcessor;
import nz.ac.auckland.integration.testing.processor.MatchedResponseProcessor;
import nz.ac.auckland.integration.testing.resource.PlainTextTestResource;
import org.apache.camel.Exchange;
import org.apache.camel.component.cxf.CxfEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Assert;
import org.junit.Test;

public class MatchResponseProcessorTest extends Assert {

    @Test
    public void testMatchedResponse() throws Exception {

        MatchedResponseProcessor.MatchedResponse response = new MatchedResponseProcessor.MatchedResponse(
                new PlainTextTestResource("foo"), new BodyProcessor("baz"));
        MatchedResponseProcessor.MatchedResponse response1 = new MatchedResponseProcessor.MatchedResponse(
                new PlainTextTestResource("baz"), new BodyProcessor("foo"));

        MatchedResponseProcessor processor = new MatchedResponseProcessor(response, response1);

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setFromEndpoint(new CxfEndpoint(""));
        e.getIn().setBody("foo");
        processor.process(e);
        assertEquals("baz", e.getIn().getBody(String.class));

        Exchange e1 = new DefaultExchange(new DefaultCamelContext());
        e1.setFromEndpoint(new CxfEndpoint(""));
        e1.getIn().setBody("baz");
        processor.process(e1);

        assertEquals("foo", e1.getIn().getBody(String.class));
        Exchange e2 = new DefaultExchange(new DefaultCamelContext());
        e2.setFromEndpoint(new CxfEndpoint(""));
        e2.getIn().setBody("moo");
        processor.process(e2);

        assertEquals("moo", e2.getIn().getBody(String.class));
    }


}
