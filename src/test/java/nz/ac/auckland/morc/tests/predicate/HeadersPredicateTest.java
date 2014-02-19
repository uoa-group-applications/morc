package nz.ac.auckland.morc.tests.predicate;

import nz.ac.auckland.morc.predicate.HeadersPredicate;
import nz.ac.auckland.morc.resource.HeadersTestResource;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

public class HeadersPredicateTest extends Assert {

    @Test
    public void testManyHeadersToString() {
        HashMap<String, Object> values = new HashMap<>();
        values.put("foo", "baz");
        values.put("abc", "baz");
        values.put("bar", "baz");
        values.put("baz", "baz");
        values.put("moo", "baz");
        values.put("cow", "baz");

        assertTrue(new HeadersPredicate(new HeadersTestResource(values)).toString().contains("..."));
    }

    @Test
    public void testSimpleToString() {
        HashMap<String, Object> values = new HashMap<>();
        values.put("foo", "baz");
        values.put("abc", "baz");

        assertTrue(new HeadersPredicate(new HeadersTestResource(values)).toString().contains("foo:baz"));
    }

}
