package nz.ac.auckland.integration.testing.expectation;

import org.apache.camel.Exchange;
import org.apache.cxf.binding.soap.SoapFault;

/**
 * This will cause a CXF endpoint (message consumer) to return a specified SOAP fault back to the consumer
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class SoapFaultMockExpectation extends SyncMockExpectation {

    public void handleReceivedExchange(Exchange exchange) throws Exception {
        exchange.getOut().setFault(true);
        super.handleReceivedExchange(exchange);
    }

    public String getType() {
        return "ws";
    }

    public static class Builder extends SyncMockExpectation.Init<SoapFaultMockExpectation, Builder,SoapFault> {

        /**
         * @param endpointUri This MUST be a CXF endpoint URI
         */
        public Builder(String endpointUri) {
            super(endpointUri);
        }

        protected SoapFaultMockExpectation buildInternal() {
            return new SoapFaultMockExpectation(this);
        }
    }

    protected SoapFaultMockExpectation(Builder builder) {
        super(builder);
    }
}

