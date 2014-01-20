package nz.ac.auckland.integration.testing.processor;

import nz.ac.auckland.integration.testing.resource.TestResource;
import nz.ac.auckland.integration.testing.predicate.Validator;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

/**
 * An answer that will return a response back to the client based on the incoming exchange message
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class MatchedResponseBodiesProcessor implements Processor {

    private static final Logger logger = LoggerFactory.getLogger(MatchedResponseBodiesProcessor.class);
    private Collection<MatchedResponse> responses = new HashSet<>();

    //todo add remove on match

    public void addMatchedResponse(MatchedResponse matchedResponse) {
        responses.add(matchedResponse);
    }

    /**
     * @param exchange  The exchange received by an expectation that will invoke a given response
     */
    @Override
    public void process(Exchange exchange) throws Exception {
        //todo make this threadsafe
        Iterator<MatchedResponse> responseIterator = responses.iterator();

        while (responseIterator.hasNext()) {
            MatchedResponse matchedResponse = responseIterator.next();
            if (matchedResponse.inputPredicate.matches(exchange)) {
                matchedResponse.responseProcessor.process(exchange);
            }
        }

        logger.warn("The exchange arriving at endpoint {} found no response match for body {}",
                exchange.getFromEndpoint(), exchange.getIn().getBody(String.class));
        exchange.getOut().setBody("");
    }

    /**
     * An approach class for making input->output pairs
     */
    public static class MatchedResponse {
        private Predicate inputPredicate;
        private Processor responseProcessor;

        public MatchedResponse(Predicate inputPredicate, Processor responseProcessor) throws Exception {
            this.inputPredicate = inputPredicate;
            this.responseProcessor = responseProcessor;
        }
    }
}
