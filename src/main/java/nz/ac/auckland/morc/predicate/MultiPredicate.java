package nz.ac.auckland.morc.predicate;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * A class for aggregating multiple predicates to appear as one such that it's easier to use outside of the builder.
 * All predicates will be evaluated, even if one fails.
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class MultiPredicate implements Predicate {
    private List<Predicate> predicates;
    private static final Logger logger = LoggerFactory.getLogger(MultiPredicate.class);

    public MultiPredicate(List<Predicate> predicates) {
        this.predicates = predicates;
    }

    @Override
    public boolean matches(Exchange exchange) {
        logger.trace("Starting validation of exchange from endpoint {} against {} predicates",
                (exchange.getFromEndpoint() != null ? exchange.getFromEndpoint().getEndpointUri() : "unknown"), predicates.size());

        boolean result = true;

        for (Predicate predicate : predicates) {
            boolean matches = predicate.matches(exchange);
            logger.trace("Result of predicate {}: {}", predicate, matches);
            if (!matches) {
                logger.warn("The predicate {} did not validate successfully - check the logs for details", predicate);
                result = false;
            }
        }

        logger.trace("Validation of {} predicates was " + (result ? "successful" : "unsuccessful") + " for endpoint {}",
                predicates.size(), (exchange.getFromEndpoint() != null ? exchange.getFromEndpoint().getEndpointUri() : "unknown"));

        return result;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("MultiPredicate: ");
        for (Predicate predicate : predicates) {
            builder.append(predicate.toString()).append(",");
        }

        String result = builder.toString();
        if (result.endsWith(",")) result = result.substring(0, result.length() - 1);

        return result;
    }

}
