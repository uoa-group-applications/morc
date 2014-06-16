package nz.ac.auckland.morc.tests.processor;

import nz.ac.auckland.morc.MorcTestBuilder;
import nz.ac.auckland.morc.processor.BodyProcessor;
import nz.ac.auckland.morc.resource.PlainTextTestResource;
import org.apache.camel.Exchange;
import org.apache.camel.component.cxf.CxfEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Assert;
import org.junit.Test;

public class BodyProcessorTest extends Assert {

    @Test
    public void testXmlRuntimeTestResource() throws Exception {
        MorcTestBuilder.XmlRuntimeTestResource resource =
                new MorcTestBuilder.XmlRuntimeTestResource(new PlainTextTestResource("<foo/>"));

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setFromEndpoint(new CxfEndpoint(""));
        BodyProcessor processor = new BodyProcessor(resource);

        processor.process(e);
        assertEquals("<foo/>", e.getIn().getBody(String.class));
    }
}
