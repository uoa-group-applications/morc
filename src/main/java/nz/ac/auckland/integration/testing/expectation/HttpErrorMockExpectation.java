package nz.ac.auckland.integration.testing.expectation;

import nz.ac.auckland.integration.testing.resource.TestResource;
import org.apache.camel.Exchange;

/**
 * This will cause a Jetty or CXF endpoint (message consumer) to throw an HTTP 500 (or other)
 * error with the specified body - this could be a SOAP fault message in the case of a web-service
 *
 * It is recommended to use SoapFaultMockExpectation if you want to throw correct CXF SOAP Faults
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class HttpErrorMockExpectation extends SyncMockExpectation {

    private int statusCode;

    public void handleReceivedExchange(Exchange exchange) throws Exception {
        exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, statusCode);
        super.handleReceivedExchange(exchange);
    }

    public String getType() {
        return "ws";
    }

    public static class Builder extends SyncMockExpectation.Init<HttpErrorMockExpectation, Builder,TestResource> {

        private int statusCode = 500;

        public Builder(String endpointUri) {
            super(endpointUri);
        }

        /**
         * @param statusCode The HTTP response code to send back to the client, defaults to 500
         */
        public Builder statusCode(int statusCode) {
            this.statusCode = statusCode;
            return self();
        }

        public HttpErrorMockExpectation build() {
            return new HttpErrorMockExpectation(this);
        }

    }

    protected HttpErrorMockExpectation(Builder builder) {
        super(builder);
        this.statusCode = builder.statusCode;
    }
}
