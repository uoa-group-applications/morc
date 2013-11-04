package nz.ac.auckland.integration.testing.expectation;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An expectation for ensuring that no message is expected to arrive at an
 * endpoint. This is only really useful on endpoints that don't receive
 * any other messages; usually to prove that the endpoint was never called
 * after a failure condition.
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class UnreceivedMockExpectation extends MockExpectation {

    private static final Logger logger = LoggerFactory.getLogger(UnreceivedMockExpectation.class);

    /**
     * This will always return false as this expectation should never be called
     */
    public boolean checkValid(Exchange incomingExchange, int index) {
        //we should not be receiving messages!
        return false;
    }

    /**
     * @throws IllegalStateException This should never have received an exchange so the test should fail
     */
    public void handleReceivedExchange(Exchange exchange) throws Exception {
        throw new IllegalStateException("Unreceived mock expectations should not be receiving messages");
    }

    public String getType() {
        return "unreceived";
    }

    public static class Builder extends MockExpectation.AbstractBuilder<UnreceivedMockExpectation, Builder> {

        public Builder(String endpointUri) {
            super(endpointUri);
        }

        protected Builder self() {
            return this;
        }

        public UnreceivedMockExpectation build() {
            this.expectedMessageCount(0);
            return new UnreceivedMockExpectation(this);
        }

    }

    protected UnreceivedMockExpectation(Builder builder) {
        super(builder);
    }
}
