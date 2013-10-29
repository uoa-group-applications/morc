package nz.ac.auckland.integration.testing.expectation;

import nz.ac.auckland.integration.testing.resource.*;
import nz.ac.auckland.integration.testing.validator.*;
import org.apache.camel.Exchange;

/**
 * An abstract class to set expectations for bodies and headers for
 * an incoming message
 * <p/>
 * This class carries out the message validation based on the test resource, expectations
 * will be returned in the order specified even if some relaxation of total ordering occurs
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public abstract class ContentMockExpectation extends MockExpectation {
    private Validator expectedBodyValidator;
    private Validator expectedHeadersValidator;

    /**
     * @return The expected headers that the target endpoint expects to receive at a particular point in test
     */
    public Validator getExpectedHeadersValidator() {
        return expectedHeadersValidator;
    }

    /**
     * @return The expected body that the target endpoint expects to receive at a particular point in the test
     */
    public Validator getExpectedBodyValidator() {
        return expectedBodyValidator;
    }

    /**
     * Evaluates whether the content coming in from an endpoint meets the content requirements. Ordering checks
     * are delegated to the superclass.
     */
    public boolean checkValid(Exchange incomingExchange, int index) {
        if (!super.checkValid(incomingExchange, index)) return false;

        //now check the expected bodies and headers are valid
        boolean validateBody = expectedBodyValidator == null || expectedBodyValidator.validate(incomingExchange);
        boolean validateHeaders = expectedHeadersValidator == null || expectedHeadersValidator.validate(incomingExchange);

        return validateBody && validateHeaders;
    }

    public static abstract class AbstractContentBuilder<Product extends MockExpectation, Builder extends AbstractBuilder<Product, Builder>> extends AbstractBuilder<Product, Builder> {

        private Validator expectedBodyValidator;
        private Validator expectedHeadersValidator;

        public AbstractContentBuilder(String endpointUri) {
            super(endpointUri);
        }

        /**
         * @param expectedBodyValidator The body that we expect to receive from the endpoint
         */
        public Builder expectedBody(Validator expectedBodyValidator) {
            this.expectedBodyValidator = expectedBodyValidator;
            return self();
        }

        /**
         * @param resource An XML resource expected to be received
         */
        public Builder expectedBody(XmlTestResource resource) {
            this.expectedBodyValidator = new XmlValidator(resource);
            return self();
        }

        /**
         * @param resource A JSON resource expected to be received
         */
        public Builder expectedBody(JsonTestResource resource) {
            this.expectedBodyValidator = new JsonValidator(resource);
            return self();
        }

        /**
         * @param resource A plain text resource expected to be received
         */
        public Builder expectedBody(PlainTextTestResource resource) {
            this.expectedHeadersValidator = new PlainTextValidator(resource);
            return self();
        }

        /**
         * @param expectedHeadersValidator A validator for the headers we expected to receive
         */
        public Builder expectedHeaders(Validator expectedHeadersValidator) {
            this.expectedHeadersValidator = expectedHeadersValidator;
            return self();
        }

        /**
         * @param resource A set of headers we expect to receive
         */
        public Builder expectedHeaders(HeadersTestResource resource) {
            this.expectedHeadersValidator = new HeadersValidator(resource);
            return self();
        }
    }

    @SuppressWarnings("unchecked")
    protected ContentMockExpectation(AbstractContentBuilder builder) {
        super(builder);

        this.expectedBodyValidator = builder.expectedBodyValidator;
        this.expectedHeadersValidator = builder.expectedHeadersValidator;
    }
}
