package nz.ac.auckland.morc.tests.predicate;

import nz.ac.auckland.morc.MorcMethods;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Assert;
import org.junit.Test;

public class JsonPathPredicateTest extends Assert implements MorcMethods {

    @Test
    public void testMatches() throws Exception {
        String message = "{ \"foo\": { \"baz\": { \"size\": 3 } } }";

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.getIn().setBody(message);

        assertTrue(jsonpath(".foo.baz[?(@.size > 1)]").matches(e));
        assertFalse(jsonpath(".foo.baz[?(@.size == 0)]").matches(e));
    }
}
