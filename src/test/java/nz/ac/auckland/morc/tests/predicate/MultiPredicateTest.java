package nz.ac.auckland.morc.tests.predicate;

import nz.ac.auckland.morc.predicate.MultiPredicate;
import org.apache.camel.Predicate;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;

public class MultiPredicateTest extends Assert {

    @Test
    public void testNoPredicatesToString() {
        assertTrue(new MultiPredicate(new ArrayList<Predicate>()).toString().startsWith("MultiPredicate"));
    }
}
