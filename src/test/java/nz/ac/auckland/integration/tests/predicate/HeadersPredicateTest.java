package nz.ac.auckland.integration.tests.predicate;

import nz.ac.auckland.integration.testing.predicate.HeadersPredicate;
import nz.ac.auckland.integration.testing.resource.HeadersTestResource;
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

        System.out.println(new HeadersPredicate(new HeadersTestResource(values)).toString());

        assertTrue(new HeadersPredicate(new HeadersTestResource(values)).toString().contains("..."));
    }
}
