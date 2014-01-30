package nz.ac.auckland.integration.testing.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

/**
 * An answer that will return a response back to the client based on the incoming exchange message
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class MatchedResponseProcessor implements Processor {

    private static final Logger logger = LoggerFactory.getLogger(MatchedResponseProcessor.class);
    private Collection<MatchedResponse> responses;

    public MatchedResponseProcessor(MatchedResponse... responses) {
        this.responses = new HashSet<>(Arrays.asList(responses));
    }

    /**
     * @param exchange The exchange received by an expectation that will invoke a given response
     *                 note that processors and predicates must be thread safe
     */
    @Override
    public void process(Exchange exchange) throws Exception {
        for (MatchedResponse matchedResponse : responses) {
            if (matchedResponse.inputPredicate.matches(exchange)) {
                logger.debug("Matched input for predicate {} at endpoint {}",
                        matchedResponse.inputPredicate, exchange.getFromEndpoint());
                matchedResponse.responseProcessor.process(exchange);
                return;
            }
        }

        logger.warn("The exchange arriving at endpoint {} found no response match for body {}",
                exchange.getFromEndpoint(), exchange.getIn().getBody(String.class));
    }

    /**
     * An approach class for making input->output pairs; these predicates and processors must be thread safe
     * as they can be called concurrently
     */
    public static class MatchedResponse {
        private Predicate inputPredicate;
        private Processor responseProcessor;

        public MatchedResponse(Predicate inputPredicate, Processor responseProcessor) {
            this.inputPredicate = inputPredicate;
            this.responseProcessor = responseProcessor;
        }
    }
}
