package nz.ac.auckland.integration.testing.expectation;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An expectation that an asynchronous message will be received at some point
 * in the future.
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class AsyncMockExpectation extends ContentMockExpectation {
    private static final Logger logger = LoggerFactory.getLogger(AsyncMockExpectation.class);

    /**
     * This is a no-op as an asynchronous expectation doesn't respond with any content
     *
     * @param exchange An exchange that has been received from a Camel endpoint
     * @throws Exception
     */
    public void handleReceivedExchange(Exchange exchange) throws Exception {
        logger.debug("Received exchange {} for async expectation {}", exchange.getExchangeId(), getName());
        //noop
    }

    public String getType() {
        return "async";
    }

    public static class Builder extends ContentMockExpectation.AbstractContentBuilder<AsyncMockExpectation, Builder> {

        public Builder(String endpointUri) {
            super(endpointUri);
        }

        protected Builder self() {
            return this;
        }

        protected AsyncMockExpectation buildInternal() {
            //setup the default as PARTIAL
            if (orderingType != OrderingType.NONE) ordering(OrderingType.PARTIAL);
            return new AsyncMockExpectation(this);
        }
    }

    protected AsyncMockExpectation(Builder builder) {
        super(builder);
    }
}
