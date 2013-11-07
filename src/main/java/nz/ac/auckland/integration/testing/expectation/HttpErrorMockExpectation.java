package nz.ac.auckland.integration.testing.expectation;

import nz.ac.auckland.integration.testing.resource.TestResource;
import org.apache.camel.Exchange;

/**
 * This will cause a CXF or Jetty endpoint (message consumer) to throw an HTTP 500
 * error with the specified body - this should be a SOAP fault message in the case of a web-service
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class HttpErrorMockExpectation extends SyncMockExpectation {

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

    public static class Builder extends SyncMockExpectation.Init<HttpErrorMockExpectation, Builder,TestResource> {

        public Builder(String endpointUri) {
            super(endpointUri);
        }

        public HttpErrorMockExpectation build() {
            return new HttpErrorMockExpectation(this);
        }
    }

    protected HttpErrorMockExpectation(Builder builder) {
        super(builder);
    }
}
