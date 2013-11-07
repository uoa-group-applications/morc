package nz.ac.auckland.integration.testing.expectation;

import nz.ac.auckland.integration.testing.resource.HeadersTestResource;
import nz.ac.auckland.integration.testing.resource.SoapFaultTestResource;
import org.apache.camel.Exchange;

/**
 * This will cause a CXF endpoint (message consumer) to return a specified SOAP fault back to the consumer
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class SoapFaultMockExpectation extends SyncMockExpectation {

    public void handleReceivedExchange(Exchange exchange) throws Exception {
        exchange.getOut().setHeader("org.apache.cxf.message.Message.RESPONSE_CODE", 500);
        exchange.getOut().setFault(true);
        super.handleReceivedExchange(exchange);
    }

    public String getType() {
        return "ws";
    }

    public static class Builder extends SyncMockExpectation.Init<SoapFaultMockExpectation, Builder,SoapFaultTestResource> {

        public Builder(String endpointUri) {
            super(endpointUri);
        }

        /**
         * @param providedResponseBody The body that should be returned back to the client which must be a
         *                             SoapFaultTestResource
         */
        @Override
        public Builder responseBody(SoapFaultTestResource providedResponseBody) {
            this.providedResponseBody = providedResponseBody;
            return self();
        }

        /**
         * @param providedResponseHeaders The headers that should be returned back to the client
         */
        @Override
        public Builder responseHeaders(HeadersTestResource providedResponseHeaders) {
            this.providedResponseHeaders = providedResponseHeaders;
            return self();
        }

        public SoapFaultMockExpectation build() {
            return new SoapFaultMockExpectation(this);
        }
    }

    protected SoapFaultMockExpectation(Builder builder) {
        super(builder);
    }
}
