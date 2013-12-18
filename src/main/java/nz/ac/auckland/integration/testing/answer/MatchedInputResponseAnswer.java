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

public class MatchedInputResponseAnswer<T> implements Answer<T> {

    private static final Logger logger = LoggerFactory.getLogger(MatchedInputResponseAnswer.class);
    private Collection<MatchedResponse<T>> responses = new HashSet<>();
    private boolean removeOnMatch = false;

    @SafeVarargs
    public MatchedInputResponseAnswer(MatchedResponse<T>... responseValues) {
        this(false, responseValues);
    }

    @SafeVarargs
    public MatchedInputResponseAnswer(boolean removeOnMatch, MatchedResponse<T>... responseValues) {
        this.removeOnMatch = removeOnMatch;
        Collections.addAll(responses, responseValues);
    }

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

        logger.warn("The exchange arriving at endpoint %s found no response match for body %s",
                exchange.getFromEndpoint(), exchange.getIn().getBody(String.class));
        return null;
    }

    public static class MatchedResponse<T> {
        private Validator inputValidator;
        private T response;

        public MatchedResponse(Validator inputValidator, TestResource<T> response) throws Exception {
            this.inputValidator = inputValidator;
            this.response = response.getValue();
        }

        public MatchedResponse(Validator inputValidator, T response) {
            this.inputValidator = inputValidator;
            this.response = response;
        }
    }
}
