package nz.ac.auckland.integration.testing.expectation;

import org.apache.camel.Exchange;

/**
 * This will cause a CXF endpoint (message consumer) to a specified SOAP fault back to the consumer
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class SoapFaultMockExpectation extends SyncMockExpectation {

    //todo: make this use a soap fault message from somewhere
    public void handleReceivedExchange(Exchange exchange) throws Exception {
        exchange.getOut().setHeader("org.apache.cxf.message.Message.RESPONSE_CODE", 500);
        super.handleReceivedExchange(exchange);
    }

    public String getType() {
        return "ws";
    }

    public static class Builder extends Init<SoapFaultMockExpectation, Builder> {

        public Builder(String endpointUri) {
            super(endpointUri);
        }

        public SoapFaultMockExpectation build() {
            return new SoapFaultMockExpectation(this);
        }
    }

    protected SoapFaultMockExpectation(Builder builder) {
        super(builder);
    }
}
