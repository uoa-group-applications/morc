package nz.ac.auckland.morc.tests.resource;

import nz.ac.auckland.morc.resource.ExceptionTestResource;
import org.apache.camel.Exchange;
import org.apache.camel.component.cxf.CxfEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.apache.cxf.binding.soap.SoapFault;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.namespace.QName;
import java.io.IOException;

public class ExceptionTestResourceTest extends Assert {

    @Test
    public void testMatchDefaultException() throws Exception {
        ExceptionTestResource predicate = new ExceptionTestResource();

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setFromEndpoint(new CxfEndpoint(""));
        e.setProperty(Exchange.EXCEPTION_CAUGHT, new Exception());

        assertTrue(predicate.matches(e));
    }

    @Test
    public void testMatchExceptionMessage() throws Exception {
        ExceptionTestResource predicate = new ExceptionTestResource(new Exception("foo"));

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setFromEndpoint(new CxfEndpoint(""));
        e.setProperty(Exchange.EXCEPTION_CAUGHT, new Exception("foo"));

        assertTrue(predicate.matches(e));
    }

    @Test
    public void testNoMatchExceptionMessage() throws Exception {
        ExceptionTestResource predicate = new ExceptionTestResource(new Exception("foo"));

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setFromEndpoint(new CxfEndpoint(""));
        e.setProperty(Exchange.EXCEPTION_CAUGHT, new Exception("baz"));

        assertFalse(predicate.matches(e));
    }

    @Test
    public void testNoMatchClassButMatchesMessage() throws Exception {
        ExceptionTestResource predicate = new ExceptionTestResource(new IOException("foo"));

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setFromEndpoint(new CxfEndpoint(""));
        e.setProperty(Exchange.EXCEPTION_CAUGHT, new Exception("foo"));

        assertFalse(predicate.matches(e));
    }

    @Test
    public void testNoMatchNullMessage() throws Exception {
        ExceptionTestResource predicate = new ExceptionTestResource(new IOException());

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setFromEndpoint(new CxfEndpoint(""));
        e.setProperty(Exchange.EXCEPTION_CAUGHT, new Exception("foo"));

        assertFalse(predicate.matches(e));
    }

    @Test
    public void testNoException() throws Exception {
        ExceptionTestResource predicate = new ExceptionTestResource(new IOException());

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setFromEndpoint(new CxfEndpoint(""));
        assertFalse(predicate.matches(e));
    }

    @Test
    public void testToStringNoMessage() throws Exception {
        ExceptionTestResource predicate = new ExceptionTestResource(new IOException());
        assertTrue(predicate.toString().contains("IOException"));
    }

    @Test
    public void testToStringWithMessage() throws Exception {
        ExceptionTestResource predicate = new ExceptionTestResource(new IOException("foo"));
        assertTrue(predicate.toString().contains("foo"));
    }

    @Test
    public void testSubClassException() throws Exception {
        SoapFault fault = new SoapFault("", new QName(""));

        ExceptionTestResource predicate = new ExceptionTestResource(new Exception());

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setFromEndpoint(new CxfEndpoint(""));
        e.setProperty(Exchange.EXCEPTION_CAUGHT, fault);

        assertTrue(predicate.matches(e));
    }

    @Test
    public void testSuperClassException() throws Exception {
        //todo
    }

}
