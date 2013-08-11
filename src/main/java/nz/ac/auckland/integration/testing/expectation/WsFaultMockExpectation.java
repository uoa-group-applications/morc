package nz.ac.auckland.integration.testing.expectation;

import org.apache.camel.Exchange;

/**
 * This will cause a CXF endpoint (message consumer) to throw an HTTP 500
 * error with the specified body - this should be a SOAP fault message
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class WsFaultMockExpectation extends SyncMockExpectation {

    /**
     * Sets the outgoing exchange header for org.apache.cxf.message.Message.RESPONSE_CODE to 500.
     * This should cause CXF to throw a SOAP Fault with the contained body
     */
    public void handleReceivedExchange(Exchange exchange) throws Exception {
        exchange.getOut().setHeader("org.apache.cxf.message.Message.RESPONSE_CODE", 500);
        //if we use Jetty instead
        exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, 500);
        super.handleReceivedExchange(exchange);
    }

    public String getType() {
        return "ws";
    }

    public static class Builder extends SyncMockExpectation.Init<WsFaultMockExpectation, Builder> {

        public Builder(String endpointUri) {
            super(endpointUri);
        }

        public WsFaultMockExpectation build() {
            return new WsFaultMockExpectation(this);
        }
    }

    protected WsFaultMockExpectation(Builder builder) {
        super(builder);
    }
}
