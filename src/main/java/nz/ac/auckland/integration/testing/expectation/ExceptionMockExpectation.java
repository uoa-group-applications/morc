package nz.ac.auckland.integration.testing.expectation;

import nz.ac.auckland.integration.testing.answer.Answer;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This endpoint will throw an exception of the specified type back to the
 * message consumer; this is likely to cause transaction rollback or some kind
 * of SOAP fault
 * <p/>
 * If no exception is specified then an empty Exception is thrown
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class ExceptionMockExpectation extends ContentMockExpectation {
    private static final Logger logger = LoggerFactory.getLogger(ExceptionMockExpectation.class);
    private Answer<Exception> exceptionResponse;

    /**
     * This will throw the specified exception back to Camel for handling as appropriate
     */
    public void handleReceivedExchange(Exchange exchange) throws Exception {
        if (exceptionResponse == null) {
            logger.info("No exception response provided for endpoint {}, a standard Exception has been used",getEndpointUri());
            throw new Exception();
        }
        throw exceptionResponse.response(exchange);
    }

    public String getType() {
        return "exception";
    }

    public static class Builder extends ContentMockExpectation.AbstractContentBuilder<ExceptionMockExpectation, Builder> {

        private Answer<Exception> exceptionResponse;

        public Builder(String endpointUri) {
            super(endpointUri);
        }

        protected Builder self() {
            return this;
        }

        public Builder exceptionResponse(Answer<Exception> response) {
            this.exceptionResponse = response;
            return self();
        }

        protected ExceptionMockExpectation buildInternal() {
            return new ExceptionMockExpectation(this);
        }
    }

    protected ExceptionMockExpectation(Builder builder) {
        super(builder);
        this.exceptionResponse = builder.exceptionResponse;
    }
}
