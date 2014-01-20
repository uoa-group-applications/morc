package nz.ac.auckland.integration.testing.predicate;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;

import java.util.List;

/**
 * User: d.macdonald@auckland.ac.nz
 * Date: 20/01/14
 */
public class MultiPredicate implements Predicate {
    private List<Predicate> predicates;

    public MultiPredicate(List<Predicate> predicates) {
        this.predicates = predicates;
    }

    @Override
    public boolean matches(Exchange exchange) {
        //todo: add logging
        for (Predicate predicate : predicates) {
            if (!predicate.matches(exchange)) return false;
        }
        return true;
    }
}
