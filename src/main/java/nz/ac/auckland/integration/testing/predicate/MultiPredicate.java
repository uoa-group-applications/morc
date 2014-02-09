package nz.ac.auckland.integration.testing.predicate;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * A class for aggregating multiple predicates to appear as one such that it's easier to use outside of the builder
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
                exchange.getFromEndpoint().getEndpointUri(), predicates.size());

        for (Predicate predicate : predicates) {
            boolean matches = predicate.matches(exchange);
            logger.trace("Result of predicate {}: {}", predicate, matches);
            if (!matches) {
                logger.warn("The predicate {} did not validate successfully - check the logs for details", predicate);
                return false;
            }
        }

        logger.trace("Validation of {} predicates was successful for endpoint {}", predicates.size(),
                exchange.getFromEndpoint().getEndpointUri());
        return true;
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
