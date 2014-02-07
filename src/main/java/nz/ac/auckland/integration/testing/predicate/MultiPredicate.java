package nz.ac.auckland.integration.testing.predicate;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;

import java.util.List;

/**
 * A class for aggregating multiple predicates to appear as one such that it's easier to use outside of the builder
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class MultiPredicate implements Predicate {
    private List<Predicate> predicates;

    public MultiPredicate(List<Predicate> predicates) {
        this.predicates = predicates;
    }

    @Override
    public boolean matches(Exchange exchange) {
        //todo: add logging, toString
        for (Predicate predicate : predicates) {
            if (!predicate.matches(exchange)) return false;
        }
        return true;
    }
}
