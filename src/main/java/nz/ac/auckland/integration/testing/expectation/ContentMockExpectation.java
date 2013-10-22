package nz.ac.auckland.integration.testing.expectation;

import nz.ac.auckland.integration.testing.resource.HeadersTestResource;
import nz.ac.auckland.integration.testing.resource.TestResource;
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
    private TestResource<String> expectedBody;
    private HeadersTestResource expectedHeaders;

    /**
     * @return The expected headers that the target endpoint expects to receive at a particular point in test
     */
    public HeadersTestResource getExpectedHeadersPath() {
        return expectedHeaders;
    }

    /**
     * @return The expected body that the target endpoint expects to receive at a particular point in the test
     */
    public TestResource<String> getExpectedBody() {
        return expectedBody;
    }

    /**
     * Evaluates whether the content coming in from an endpoint meets the content requirements. Ordering checks
     * are delegated to the superclass.
     */
    public boolean checkValid(Exchange incomingExchange, int index) {
        if (!super.checkValid(incomingExchange, index)) return false;

        //now check the expected bodies and headers are valid
        boolean validateBody = expectedBody == null || expectedBody.validate(incomingExchange.getIn().getBody(String.class));
        boolean validateHeaders = expectedHeaders == null || expectedHeaders.validate(incomingExchange.getIn().getHeaders());

        return validateBody && validateHeaders;
    }

    public static abstract class AbstractContentBuilder<Product extends MockExpectation, Builder extends AbstractBuilder<Product, Builder>> extends AbstractBuilder<Product, Builder> {

        private TestResource<String> expectedBody;
        private HeadersTestResource expectedHeaders;

        public AbstractContentBuilder(String endpointUri) {
            super(endpointUri);
        }

        /**
         * @param expectedBody The body that we expect to receive from the endpoint
         */
        public Builder expectedBody(TestResource<String> expectedBody) {
            this.expectedBody = expectedBody;
            return self();
        }

        /**
         * @param expectedHeaders The headers that we expect to receive from the endpoint
         */
        public Builder expectedHeaders(HeadersTestResource expectedHeaders) {
            this.expectedHeaders = expectedHeaders;
            return self();
        }
    }

    @SuppressWarnings("unchecked")
    protected ContentMockExpectation(AbstractContentBuilder builder) {
        super(builder);

        this.expectedBody = builder.expectedBody;
        this.expectedHeaders = builder.expectedHeaders;
    }
}
