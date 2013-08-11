package nz.ac.auckland.integration.testing.expectation;

import org.apache.camel.Exchange;
import org.apache.camel.util.URISupport;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;

/**
 * A general class for specifying an expectation for a consumer of the
 * given endpoint URI. Each expectation can have a name (to distinguish
 * between expectations on the same endpoint).
 * <p/>
 * By default messages are expected to be in strict order (they arrive in the
 * order that they are defined in the test). This can be relaxed by setting the
 * orderingType to something other than TOTAL; PARTIAL ordering will allow messages
 * to arrive at a point in time after they're expected (useful for asynchronous messaging)
 * and also NONE which will accept a matching expectation at any point in the test. It is
 * also possible to set endpoint ordering to false such that messages can arrive in any order
 * - this may be useful when you just care about a message arriving, and not providing a
 * meaningful/ordered response.
 * <p/>
 * By default, it is expected that each expectation will occur only
 * once on a given endpoint; by setting expectedMessageCount we can repeat
 * the same expectation multiple times following the ordering parameters above
 * (e.g. by default, an expectedMessageCount of 3 would expect 3 messages in
 * a row without messages arriving from any other endpoint).
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public abstract class MockExpectation {
    private String endpointUri;
    private int receivedAt;
    private int expectedMessageCount = 1;
    private String name;
    private OrderingType orderingType;
    private boolean isEndpointOrdered = true;

    public enum OrderingType {
        TOTAL,
        PARTIAL,
        NONE
    }

    /**
     * @return A name that identifies the expectation (not necessarily unique)
     */
    public String getName() {
        String msg = "(Endpoint: " + endpointUri + ", Expected Message Count: " + expectedMessageCount +
                ", Starting Index: " + receivedAt + ")";
        if (name != null) return name + ":" + msg;

        return msg;
    }

    /**
     * @return The ordering type this expectation requires to be satisfied; the default is TOTAL
     *         which means it must arrive in the exact order it was specified. PARTIAL means it must
     *         arrive after it was defined. NONE means that it can arrive at any time at all during the
     *         test. The actual test execution will manage these differing ordering requirements between
     *         the different endpoints
     */
    public OrderingType getOrderingType() {
        return orderingType;
    }

    /**
     * @return The Camel-formatted URI that should be listened to for messages and requests
     */
    public String getEndpointUri() {
        return endpointUri;
    }

    /**
     * @return The point in time that we expect to receive the message in the context of this test
     */
    public int getReceivedAt() {
        return receivedAt;
    }

    /**
     * @return The number of messages that the endpoint is expected to receive for this test (in order)
     */
    public int getExpectedMessageCount() {
        return expectedMessageCount;
    }

    /**
     * @return Whether messages arriving at this endpoint have to arrive in order
     */
    public boolean isEndpointOrdered() {
        return isEndpointOrdered;
    }

    public String toString() {
        return getName();
    }

    /**
     * @return true if the message has been received in the correct order based on the expected receivedAt value
     *         and the ordering requirements.
     */
    public boolean checkValid(Exchange incomingExchange, int index) {

        //ensure we're working with an exchange that should be for this endpoint
        if (!incomingExchange.getFromEndpoint().equals(incomingExchange.getContext().getEndpoint(endpointUri)))
            return false;

        if (orderingType == OrderingType.NONE) return true;

        if (orderingType == OrderingType.TOTAL && index >= receivedAt && index <= (receivedAt + expectedMessageCount - 1))
            return true;

        //partially ordered exchanges can occur in the future
        return (orderingType == OrderingType.PARTIAL && index >= receivedAt);

    }

    /**
     * This is what is called by the test once a message has arrived at an endpoint. It is useful in setting the
     * outgoing response in the case of a synchronous expectation
     *
     * @param exchange The Camel exchange that needs to be modified, or handled once it has been received
     * @throws Exception
     */
    public abstract void handleReceivedExchange(Exchange exchange) throws Exception;

    /**
     * @return The type of expectation
     */
    public abstract String getType();

    /*
        Using details from: https://weblogs.java.net/node/642849
     */
    public static abstract class AbstractBuilder<Product extends MockExpectation, Builder extends AbstractBuilder<Product, Builder>> {
        protected String endpointUri;
        protected int receivedAt;
        protected int expectedMessageCount = 1;
        protected OrderingType orderingType = OrderingType.TOTAL;
        protected boolean isEndpointOrdered = true;
        protected String name;

        protected abstract Builder self();

        public abstract Product build();

        public AbstractBuilder(String endpointUri) {
            try {
                this.endpointUri = URISupport.normalizeUri(endpointUri);
            } catch (URISyntaxException | UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * @param name The name of the expectation for identification on failure
         */
        public Builder name(String name) {
            this.name = name;
            return self();
        }

        /**
         * @param receivedAt The point in time that this expectation should be received; note that the specification
         *                   builder will override this with it's own internal ordering
         */
        public Builder receivedAt(int receivedAt) {
            this.receivedAt = receivedAt;
            return self();
        }

        /**
         * @param expectedMessageCount The number of messages that we expect to receive for this expectation
         */
        public Builder expectedMessageCount(int expectedMessageCount) {
            this.expectedMessageCount = expectedMessageCount;
            return self();
        }

        /**
         * Specifies the ordering that this expectation requires (TOTAL, PARTIAL or NONE)
         */
        public Builder ordering(OrderingType orderingType) {
            this.orderingType = orderingType;
            return self();
        }

        /**
         * Specifies whether the endpoint expects messages in the order that they are defined.
         */
        public Builder endpointNotOrdered() {
            isEndpointOrdered = false;
            return self();
        }
    }

    protected MockExpectation(AbstractBuilder builder) {
        this.name = builder.name;
        this.endpointUri = builder.endpointUri;
        this.receivedAt = builder.receivedAt;
        this.expectedMessageCount = builder.expectedMessageCount;
        this.orderingType = builder.orderingType;
        this.isEndpointOrdered = builder.isEndpointOrdered;
    }

}
