package nz.ac.auckland.integration.testing.expectation;

import nz.ac.auckland.integration.testing.resource.HeadersTestResource;
import nz.ac.auckland.integration.testing.resource.TestResource;
import org.apache.camel.Exchange;

/**
 * An expectation that provides a message response back to the message
 * consumer
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class SyncMockExpectation extends ContentMockExpectation {
    private TestResource providedResponseBody;
    private HeadersTestResource providedResponseHeaders;

    /**
     * @return The response headers that this expectation will return to the endpoint
     */
    public HeadersTestResource getProvidedResponseHeaders() {
        return providedResponseHeaders;
    }

    /**
     * @return The response body that the expectation will return to the endpoint
     */
    public TestResource getProvidedResponseBody() {
        return providedResponseBody;
    }

    /**
     * Sets the exchange out body to the provided response body and headers
     *
     * @param exchange The Camel exchange that needs to be modified, or handled once it has been received
     * @throws Exception
     */
    public void handleReceivedExchange(Exchange exchange) throws Exception {
        if (providedResponseBody != null) exchange.getOut().setBody(providedResponseBody.getValue());
        else exchange.getOut().setBody("");
        if (providedResponseHeaders != null) exchange.getOut().setHeaders(providedResponseHeaders.getValue());
    }

    public String getType() {
        return "sync";
    }

    public static class Builder extends Init<SyncMockExpectation, Builder> {

        public Builder(String endpointUri) {
            super(endpointUri);
        }

        public SyncMockExpectation build() {
            return new SyncMockExpectation(this);
        }
    }

    protected abstract static class Init<Product, Builder extends Init<Product, Builder>> extends ContentMockExpectation.AbstractContentBuilder<SyncMockExpectation, Builder> {

        protected TestResource providedResponseBody;
        protected HeadersTestResource providedResponseHeaders;

        public Init(String endpointUri) {
            super(endpointUri);
        }

        /**
         * @param providedResponseBody The body that should be returned back to the client
         */
        public Builder responseBody(TestResource providedResponseBody) {
            this.providedResponseBody = providedResponseBody;
            return self();
        }

        /**
         * @param providedResponseHeaders The headers that should be returned back to the client
         */
        public Builder responseHeaders(HeadersTestResource providedResponseHeaders) {
            this.providedResponseHeaders = providedResponseHeaders;
            return self();
        }

        @SuppressWarnings("unchecked")
        protected Builder self() {
            //this may through an exception if the implementation isn't complete
            return (Builder) this;
        }
    }

    @SuppressWarnings("unchecked")
    protected SyncMockExpectation(Init builder) {
        super(builder);

        this.providedResponseBody = builder.providedResponseBody;
        this.providedResponseHeaders = builder.providedResponseHeaders;
    }

}
