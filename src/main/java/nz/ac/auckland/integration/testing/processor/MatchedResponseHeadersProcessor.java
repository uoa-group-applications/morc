package nz.ac.auckland.integration.testing.processor;

import nz.ac.auckland.integration.testing.resource.TestResource;
import nz.ac.auckland.integration.testing.validator.Validator;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/**
 * An answer that will return a response back to the client based on the incoming exchange message
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class MatchedResponseHeadersProcessor implements Processor {

    private static final Logger logger = LoggerFactory.getLogger(MatchedResponseHeadersProcessor.class);
    private Collection<MatchedResponse> responses = new HashSet<>();

    public void addMatchedResponse(MatchedResponse matchedResponse) {
        responses.add(matchedResponse);
    }

    /**
     * @param exchange  The exchange received by an expectation that will invoke a given response
     */
    @Override
    public void process(Exchange exchange) throws Exception {
        Iterator<MatchedResponse> responseIterator = responses.iterator();

        while (responseIterator.hasNext()) {
            MatchedResponse matchedResponse = responseIterator.next();
            if (matchedResponse.inputValidator.validate(exchange,0)) {
                exchange.getOut().setHeaders(matchedResponse.response);
            }
        }

        logger.warn("The exchange arriving at endpoint {} found no match ",
                exchange.getFromEndpoint(), exchange.getIn().getBody(String.class));
    }

    /**
     * An approach class for making input->output pairs
     */
    public static class MatchedResponse {
        private Validator inputValidator;
        private Map<String,Object> response;

        /**
         * @param inputValidator    A way of validating the input as what we expect for a match
         * @param response          A test resource response for this given input match
         */
        public MatchedResponse(Validator inputValidator, TestResource<Map<String,Object>> response) throws Exception {
            this.inputValidator = inputValidator;
            this.response = response.getValue();
        }

        /**
         * @param inputValidator    A way of validating the input as what we expect for a match
         * @param response          A response for this given input match
         */
        public MatchedResponse(Validator inputValidator, Map<String,Object> response) {
            this.inputValidator = inputValidator;
            this.response = response;
        }
    }
}
