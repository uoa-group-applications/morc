package nz.ac.auckland.morc.tests.predicate;

import nz.ac.auckland.morc.predicate.ExceptionPredicate;
import org.apache.camel.Exchange;
import org.apache.camel.component.cxf.CxfEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class ExceptionPredicateTest extends Assert {

    @Test
    public void testMatchDefaultException() throws Exception {
        ExceptionPredicate predicate = new ExceptionPredicate();

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setFromEndpoint(new CxfEndpoint(""));
        e.setProperty(Exchange.EXCEPTION_CAUGHT, new Exception());

        assertTrue(predicate.matches(e));
    }

    @Test
    public void testMatchExceptionMessage() throws Exception {
        ExceptionPredicate predicate = new ExceptionPredicate(Exception.class, "foo");

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setFromEndpoint(new CxfEndpoint(""));
        e.setProperty(Exchange.EXCEPTION_CAUGHT, new Exception("foo"));

        assertTrue(predicate.matches(e));
    }

    @Test
    public void testNoMatchExceptionMessage() throws Exception {
        ExceptionPredicate predicate = new ExceptionPredicate(Exception.class, "foo");

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setFromEndpoint(new CxfEndpoint(""));
        e.setProperty(Exchange.EXCEPTION_CAUGHT, new Exception("baz"));

        assertFalse(predicate.matches(e));
    }

    @Test
    public void testNoMatchClassButMatchesMessage() throws Exception {
        ExceptionPredicate predicate = new ExceptionPredicate(IOException.class, "foo");

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setFromEndpoint(new CxfEndpoint(""));
        e.setProperty(Exchange.EXCEPTION_CAUGHT, new Exception("foo"));

        assertFalse(predicate.matches(e));
    }

    @Test
    public void testNoMatchNullMessage() throws Exception {
        ExceptionPredicate predicate = new ExceptionPredicate(IOException.class);

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setFromEndpoint(new CxfEndpoint(""));
        e.setProperty(Exchange.EXCEPTION_CAUGHT, new Exception("foo"));

        assertFalse(predicate.matches(e));
    }

    @Test
    public void testNoException() throws Exception {
        ExceptionPredicate predicate = new ExceptionPredicate(IOException.class);

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setFromEndpoint(new CxfEndpoint(""));
        assertFalse(predicate.matches(e));
    }

    @Test
    public void testToStringNoMessage() throws Exception {
        ExceptionPredicate predicate = new ExceptionPredicate(IOException.class);
        assertTrue(predicate.toString().contains("IOException"));
    }

    @Test
    public void testToStringWithMessage() throws Exception {
        ExceptionPredicate predicate = new ExceptionPredicate(IOException.class, "foo");
        assertTrue(predicate.toString().contains("foo"));
    }
}
