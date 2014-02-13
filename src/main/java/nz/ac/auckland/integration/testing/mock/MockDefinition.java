package nz.ac.auckland.integration.testing.mock;

import nz.ac.auckland.integration.testing.MorcBuilder;
import nz.ac.auckland.integration.testing.endpointoverride.EndpointOverride;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.model.RouteDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A general class for declaring a mock definition for a consumer of the
 * given endpoint URI.
 * <p/>
 * By default messages are expected to be in strict order (they arrive in the
 * order that they are defined in the test). This can be relaxed by setting the
 * orderingType to something other than TOTAL; PARTIAL ordering will allow messages
 * to arrive at a point in time after they're expected (useful for asynchronous messaging)
 * and also NONE which will accept a matching expectation at any point in the test. It is
 * also possible to set endpoint ordering to false such that messages can arrive in any order
 * - this may be useful when you just care about a message arriving, and not providing a
 * meaningful/ordered response.
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class MockDefinition {

    private static final Logger logger = LoggerFactory.getLogger(MockDefinition.class);

    private String endpointUri;
    private boolean isEndpointOrdered = true;
    private OrderingType orderingType;
    private List<Processor> processors;
    private List<Predicate> predicates;
    private int expectedMessageCount;
    private RouteDefinition mockFeederRoute;
    private Predicate lenientSelector;
    private LenientProcessor lenientProcessor;
    private long messageResultWaitTime;
    private Collection<EndpointOverride> endpointOverrides = new ArrayList<>();
    private long reassertionPeriod;
    private long minimalResultWaitTime;

    public enum OrderingType {
        TOTAL,
        PARTIAL,
        NONE
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
     * @return Whether messages arriving at this endpoint have to arrive in order
     */
    public boolean isEndpointOrdered() {
        return isEndpointOrdered;
    }

    /**
     * @return A list of processors that will be applied to incoming messages
     */
    public List<Processor> getProcessors() {
        return Collections.unmodifiableList(processors);
    }

    /**
     * @return A list of validators for validating the messages arriving at the endpoint
     */
    public List<Predicate> getPredicates() {
        return Collections.unmodifiableList(predicates);
    }

    /**
     * @return The number of messages that are expected to arrive at this endpoint
     */
    public int getExpectedMessageCount() {
        return expectedMessageCount;
    }

    /**
     * @return The route that is used to provide the mock validator with messages
     */
    public RouteDefinition getMockFeederRoute() {
        return mockFeederRoute;
    }

    /**
     * @return a predicate that decides whether an incoming message should be validated against, or passed over
     *         and simply processed with an appropriate response
     */
    public Predicate getLenientSelector() {
        return lenientSelector;
    }

    /**
     * @return A processor that selects a processor to be used to generate a response for lenient/unvalidated messages
     */
    public LenientProcessor getLenientProcessor() {
        return lenientProcessor;
    }

    /**
     * @return The maximum time per message that the mock will wait to receive and validate a message
     */
    protected long getMessageResultWaitTime() {
        return messageResultWaitTime;
    }

    /**
     * @return The set of overrides that will modify the definition's endpoint
     */
    public Collection<EndpointOverride> getEndpointOverrides() {
        return endpointOverrides;
    }

    /**
     * @return The maximum time in milliseconds the mock will wait to receive all messages
     */
    public long getResultWaitTime() {
        //minimalResultWaitTime gives the route time to boot
        return minimalResultWaitTime + (messageResultWaitTime * expectedMessageCount);
    }

    /**
     * @return The amount of time in milliseconds that the mock will wait after successfully asserting all messages
     *         arrived correctly before asserting again that the endpoint is still correct - this is useful for
     *         providing extra time to ensure no additional unexpected messages arrive at the endpoint
     */
    public long getReassertionPeriod() {
        //if expected message count is 0 then wait time will fall straight through latch, we need to reassert for 10s to
        //ensure no further messages arrive
        if (expectedMessageCount == 0) return getResultWaitTime();
        return reassertionPeriod;
    }

    /**
     * A concrete implementation of MockDefinitionBuilderInit
     */
    public static class MockDefinitionBuilder extends MockDefinitionBuilderInit<MockDefinitionBuilder> {
        /**
         * @param endpointUri A Camel Endpoint URI to listen to for expected messages
         */
        public MockDefinitionBuilder(String endpointUri) {
            super(endpointUri);
        }
    }

    public static class MockDefinitionBuilderInit<Builder extends MockDefinitionBuilderInit<Builder>> extends MorcBuilder<Builder> {

        private OrderingType orderingType = OrderingType.TOTAL;
        private boolean isEndpointOrdered = true;
        private Predicate lenientSelector = null;
        private int expectedMessageCount = 0;
        private RouteDefinition mockFeederRoute = null;
        private Class<? extends LenientProcessor> lenientProcessorClass = LenientProcessor.class;
        private LenientProcessor lenientProcessor;
        private long reassertionPeriod = 0;

        //these will be populated during the build
        private List<Predicate> predicates;
        private List<Processor> processors;

        /**
         * @param endpointUri A Camel Endpoint URI to listen to for expected messages
         */
        public MockDefinitionBuilderInit(String endpointUri) {
            super(endpointUri);
        }

        /**
         * @param expectedMessageCount Specifies the number of messages this endpoint expects to receive - if this is
         *                             greater than the number of predicates provided then any subsequent messages will
         *                             be accepted without any validation
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

        /**
         * Specifies that this endpoint mock definition is to be lenient - all messages in this part will not be validated
         * and have processors applied in order to provide a response. Only one mock definition part for an endpoint URI
         * can be specified as lenient
         */
        public Builder lenient() {
            return lenient(new Predicate() {
                @Override
                public boolean matches(Exchange exchange) {
                    return true;
                }

                @Override
                public String toString() {
                    return "TrueLenientPredicate";
                }
            });
        }

        /**
         * @param lenientSelector A selector that decides whether an exchange should be processed leniently (without
         *                        validation), and using the provided processors to generate a response. Only one mock
         *                        definition part for an endpoint URI can be specified as lenient
         */
        public Builder lenient(Predicate lenientSelector) {
            this.lenientSelector = lenientSelector;
            return self();
        }

        /**
         * @param lenientProcessorClass A processor that selects a processor for lenient (unvalidated) exchange processing
         *                              - the default implementation takes a modulus approach
         */
        public Builder lenientProcessor(Class<? extends LenientProcessor> lenientProcessorClass) {
            this.lenientProcessorClass = lenientProcessorClass;
            return self();
        }

        /**
         * @param mockFeederRoute A route definition that will be used as the intermediate between receiving a message
         *                        from the mock definition endpoint, and passing it through to the mock endpoint. Useful
         *                        for adding additional exchange operations on message arrival
         */
        public Builder mockFeederRoute(RouteDefinition mockFeederRoute) {
            this.mockFeederRoute = mockFeederRoute;
            return self();
        }

        /**
         * @param reassertionPeriod The amount of time in milliseconds that the mock will wait after successfully
         *                          asserting all messages arrived correctly before asserting again that the endpoint
         *                          is still correct - this is useful for providing extra time to ensure no additional
         *                          unexpected messages arrive at the endpoint
         */
        public Builder reassertionPeriod(long reassertionPeriod) {
            this.reassertionPeriod = reassertionPeriod;
            return self();
        }

        public MockDefinition build(MockDefinition previousDefinitionPart) {

            if (expectedMessageCount < 0)
                throw new IllegalArgumentException("The expected message count for the mock definition on endpoint "
                        + getEndpointUri() + " must be at least 0");

            if (previousDefinitionPart == null) {
                //set up a default expectation feeder route (sending to a mock will be added later)
                if (mockFeederRoute == null) mockFeederRoute = new RouteDefinition().convertBodyTo(String.class);
                mockFeederRoute.from(getEndpointUri());
            }

            if (lenientSelector == null) {
                if (!lenientProcessorClass.equals(LenientProcessor.class))
                    throw new IllegalArgumentException("The mock definition for endpoint " + getEndpointUri() +
                     " can only specify a lenient processor when a lenient selector is provided");

                predicates = getPredicates(expectedMessageCount);
                expectedMessageCount = Math.max(predicates.size(),expectedMessageCount);
                processors = getProcessors(expectedMessageCount);

                logger.debug("Creating mock definition part for endpoint {} with {} processors and {} predicates", new Object[]{
                        getEndpointUri(), processors.size(), predicates.size()});
            } else {
                if (expectedMessageCount > 0 || getPredicates().size() > 0)
                    logger.warn("Expectations and predicates for a lenient endpoint part {} will be ignored", getEndpointUri());

                expectedMessageCount = 0;

                try {
                    lenientProcessor = lenientProcessorClass.getDeclaredConstructor(List.class)
                            .newInstance(Collections.unmodifiableList(getProcessors()));
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                        | NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }

                predicates = new ArrayList<>();
                processors = new ArrayList<>();

                logger.debug("Creating lenient mock definition part for endpoint {}",getEndpointUri());
            }

            if (previousDefinitionPart != null) {
                if (!previousDefinitionPart.getEndpointUri().equals(getEndpointUri()))
                    throw new IllegalArgumentException("The endpoints do not match for merging mock definition endpoint " +
                            previousDefinitionPart.getEndpointUri() + " with endpoint " + getEndpointUri());

                if (mockFeederRoute != null)
                    throw new IllegalArgumentException("The mock feeder route for the endpoint " + getEndpointUri() +
                            " can only be specified in the first mock definition part");

                if (lenientSelector != null && previousDefinitionPart.lenientSelector != null)
                    throw new IllegalArgumentException(getEndpointUri() + " can have only one part of a mock endpoint defined as lenient");

                if (lenientSelector == null) {
                    if (previousDefinitionPart.isEndpointOrdered() != isEndpointOrdered)
                        throw new IllegalArgumentException("The endpoint ordering must be the same for all mock definition parts of " +
                                "endpoint " + getEndpointUri());

                    if (previousDefinitionPart.getOrderingType() != orderingType)
                        throw new IllegalArgumentException("The ordering type must be same for all mock definition parts on the endpoint "
                                + getEndpointUri());

                    if (previousDefinitionPart.getMessageResultWaitTime() != getMessageResultWaitTime())
                        logger.warn("The result waiting time for a subsequent mock definition part on endpoint {} has a different " +
                                "time - the first will be used and will apply to the endpoint as a whole", getEndpointUri());

                    if (previousDefinitionPart.minimalResultWaitTime != getMinimalResultWaitTime())
                        logger.warn("The minimal result wait time for a subsequent mock definition part on endpoint {} has a different " +
                                "time - the first will be used and will apply to the endpoint as a whole", getEndpointUri());

                    if (previousDefinitionPart.reassertionPeriod != reassertionPeriod)
                        logger.warn("The reassertion period for a subsequent mock definition part on endpoint {} has a different " +
                                "time - the first will be used and will apply to the endpoint as a whole", getEndpointUri());

                    predicates.addAll(0, previousDefinitionPart.getPredicates());
                    processors.addAll(0, previousDefinitionPart.getProcessors());

                    this.lenientSelector = previousDefinitionPart.lenientSelector;
                    this.lenientProcessor = previousDefinitionPart.lenientProcessor;
                } else {
                    predicates = previousDefinitionPart.getPredicates();
                    processors = previousDefinitionPart.getProcessors();
                }

                this.orderingType = previousDefinitionPart.orderingType;
                this.isEndpointOrdered = previousDefinitionPart.isEndpointOrdered;
                reassertionPeriod(previousDefinitionPart.reassertionPeriod);
                minimalResultWaitTime(previousDefinitionPart.minimalResultWaitTime);
                messageResultWaitTime(previousDefinitionPart.getMessageResultWaitTime());
                this.expectedMessageCount += previousDefinitionPart.getExpectedMessageCount();
                this.mockFeederRoute = previousDefinitionPart.getMockFeederRoute();
                for (EndpointOverride endpointOverride : previousDefinitionPart.getEndpointOverrides()) {
                    this.addEndpointOverride(endpointOverride);
                }
            }

            return new MockDefinition(this);
        }

        protected Builder self() {
            return (Builder) this;
        }

        protected OrderingType getOrderingType() {
            return this.orderingType;
        }
    }

    //any processors added should be thread safe!
    public static class LenientProcessor implements Processor {
        private List<Processor> processors;
        private AtomicInteger messageIndex = new AtomicInteger(0);

        public LenientProcessor(List<Processor> processors) {
            this.processors = processors;
        }

        public void process(Exchange exchange) throws Exception {
            if (processors.size() == 0) return;

            //the default implementation will cycle through the responses/partProcessors
            processors.get(messageIndex.getAndIncrement() % processors.size()).process(exchange);
        }
    }

    @SuppressWarnings("unchecked")
    private MockDefinition(MockDefinitionBuilderInit builder) {
        this.endpointUri = builder.getEndpointUri();
        this.orderingType = builder.orderingType;
        this.isEndpointOrdered = builder.isEndpointOrdered;
        this.processors = builder.processors;
        this.predicates = builder.predicates;
        this.expectedMessageCount = builder.expectedMessageCount;
        this.lenientProcessor = builder.lenientProcessor;
        this.lenientSelector = builder.lenientSelector;
        this.endpointOverrides = builder.getEndpointOverrides();
        this.mockFeederRoute = builder.mockFeederRoute;
        this.messageResultWaitTime = builder.getMessageResultWaitTime();
        this.reassertionPeriod = builder.reassertionPeriod;
        this.minimalResultWaitTime = builder.getMinimalResultWaitTime();
    }
}
