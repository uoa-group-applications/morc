package nz.ac.auckland.integration.testing.answer;

import nz.ac.auckland.integration.testing.resource.TestResource;
import nz.ac.auckland.integration.testing.validator.Validator;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

/**
 * An answer that will return a response back to the client based on the incoming exchange message
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class MatchedInputResponseAnswer<T> implements Answer<T> {

    private static final Logger logger = LoggerFactory.getLogger(MatchedInputResponseAnswer.class);
    private Collection<MatchedResponse<T>> responses = new HashSet<>();
    private boolean removeOnMatch = false;

    /**
     * @param responseValues A collection of input->output pairs for responses
     */
    @SafeVarargs
    public MatchedInputResponseAnswer(MatchedResponse<T>... responseValues) {
        this(false, responseValues);
    }

    /**
     * @param removeOnMatch     When a match is made, the output will be returned and the match will be removed
     * @param responseValues    A collection of input->output pairs for responses
     */
    @SafeVarargs
    public MatchedInputResponseAnswer(boolean removeOnMatch, MatchedResponse<T>... responseValues) {
        this.removeOnMatch = removeOnMatch;
        Collections.addAll(responses, responseValues);
    }

    /**
     * @param exchange  The exchange received by an expectation that will invoke a given response
     */
    @Override
    public synchronized T response(Exchange exchange) throws Exception {
        Iterator<MatchedResponse<T>> responseIterator = responses.iterator();

        while (responseIterator.hasNext()) {
            MatchedResponse<T> matchedResponse = responseIterator.next();
            if (matchedResponse.inputValidator.validate(exchange)) {
                if (removeOnMatch) responseIterator.remove();
                return matchedResponse.response;
            }
        }

        logger.warn("The exchange arriving at endpoint {} found no response match for body {}",
                exchange.getFromEndpoint(), exchange.getIn().getBody(String.class));
        return null;
    }

    /**
     * An approach class for making input->output pairs
     */
    public static class MatchedResponse<T> {
        private Validator inputValidator;
        private T response;

        /**
         * @param inputValidator    A way of validating the input as what we expect for a match
         * @param response          A test resource response for this given input match
         */
        public MatchedResponse(Validator inputValidator, TestResource<T> response) throws Exception {
            this.inputValidator = inputValidator;
            this.response = response.getValue();
        }

        /**
         * @param inputValidator    A way of validating the input as what we expect for a match
         * @param response          A response for this given input match
         */
        public MatchedResponse(Validator inputValidator, T response) {
            this.inputValidator = inputValidator;
            this.response = response;
        }
    }
}
