package nz.ac.auckland.morc.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * An answer that will return a response back to the client based on the incoming exchange message. If no match
 * is found then the default processor is applied
 *
 * @author David MacDonald - d.macdonald@auckland.ac.nz
 */
public class MatchedResponseProcessor implements Processor {

    private static final Logger logger = LoggerFactory.getLogger(MatchedResponseProcessor.class);
    private Collection<MatchedResponse> responses;
    private DefaultMatchedResponse defaultResponse;

    public MatchedResponseProcessor(MatchedResponse... responses) {
        this.responses = new HashSet<>(Arrays.asList(responses));
    }

    public MatchedResponseProcessor(DefaultMatchedResponse defaultResponse, MatchedResponse... responses) {
        this(responses);
        this.defaultResponse = defaultResponse;
    }

    /**
     * @param exchange The exchange received by an mock definition that will invoke a given response
     *                 - note that processors and predicates must be thread safe
     */
    @Override
    public void process(Exchange exchange) throws Exception {
        for (MatchedResponse matchedResponse : responses) {
            if (matchedResponse.inputPredicate.matches(exchange)) {
                logger.debug("Matched input for predicate {} at endpoint {}", matchedResponse.inputPredicate,
                        (exchange.getFromEndpoint() != null ? exchange.getFromEndpoint().getEndpointUri() : "unknown"));

                for (Processor e : matchedResponse.responseProcessors)
                    e.process(exchange);

                return;
            }
        }

        if (defaultResponse != null) {
            logger.debug("The exchange arriving at endpoint {} found no response match for body {}",
                    (exchange.getFromEndpoint() != null ? exchange.getFromEndpoint().getEndpointUri() : "unknown")
                    , exchange.getIn().getBody(String.class));

            for (Processor processor : defaultResponse.responseProcessors)
                processor.process(exchange);

        } else logger.warn("The exchange arriving at endpoint {} found no response match for body {}",
                (exchange.getFromEndpoint() != null ? exchange.getFromEndpoint().getEndpointUri() : "unknown"),
                exchange.getIn().getBody(String.class));
    }

    /**
     * An approach class for making input to output pairs; these predicates and processors must be thread safe
     * as they can be called concurrently
     */
    public static class MatchedResponse {
        private Predicate inputPredicate;
        private List<Processor> responseProcessors;

        public MatchedResponse(Predicate inputPredicate, Processor processor, Processor... responseProcessors) {
            this.inputPredicate = inputPredicate;
            ArrayList<Processor> processors = new ArrayList<>(Arrays.asList(responseProcessors));
            processors.add(0, processor);
            this.responseProcessors = Collections.unmodifiableList(processors);
        }
    }

    public static class DefaultMatchedResponse {
        private List<Processor> responseProcessors;

        public DefaultMatchedResponse(Processor... responseProcessors) {
            this.responseProcessors = Collections.unmodifiableList(Arrays.asList(responseProcessors));
        }
    }
}
