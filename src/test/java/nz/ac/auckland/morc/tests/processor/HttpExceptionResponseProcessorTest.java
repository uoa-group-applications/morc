package nz.ac.auckland.morc.tests.processor;

import nz.ac.auckland.morc.processor.HttpExceptionResponseProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.component.http.HttpOperationFailedException;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HttpExceptionResponseProcessorTest extends Assert {

    @Test
    public void testNoException() throws Exception {
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        HttpExceptionResponseProcessor processor = new HttpExceptionResponseProcessor();
        processor.process(e);
        assertNull(e.getIn().getBody());
        assertNull(e.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));
    }

    @Test
    public void testWrongException() throws Exception {
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setProperty(Exchange.EXCEPTION_CAUGHT,new IOException());
        HttpExceptionResponseProcessor processor = new HttpExceptionResponseProcessor();
        processor.process(e);
        assertNull(e.getIn().getBody());
        assertNull(e.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));
    }

    @Test
    public void testSettingContent() throws Exception {
        Exchange e = new DefaultExchange(new DefaultCamelContext());

        //check headers are appended
        e.getIn().setHeader("foo","baz");

        HttpExceptionResponseProcessor processor = new HttpExceptionResponseProcessor();
        Map<String,String> headers = new HashMap<>();
        headers.put("a","b");
        headers.put("c","d");

        Exception ex = new HttpOperationFailedException("foo",505,"failbot","somewhere",headers,"responsebody");
        e.setProperty(Exchange.EXCEPTION_CAUGHT,ex);

        processor.process(e);

        assertEquals("responsebody",e.getIn().getBody(String.class));
        assertEquals(505,e.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));
        assertEquals("b",e.getIn().getHeader("a"));
        assertEquals("d",e.getIn().getHeader("c"));
        assertEquals("baz",e.getIn().getHeader("foo"));
    }

}
