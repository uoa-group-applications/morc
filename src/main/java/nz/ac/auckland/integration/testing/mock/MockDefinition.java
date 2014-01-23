package nz.ac.auckland.integration.testing.mock;

import nz.ac.auckland.integration.testing.predicate.MultiPredicate;
import nz.ac.auckland.integration.testing.processor.MultiProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.util.URISupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
    private long assertionTime;

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

    public List<Processor> getProcessors() {
        return Collections.unmodifiableList(processors);
    }

    public List<Predicate> getPredicates() {
        return Collections.unmodifiableList(predicates);
    }

    public int getExpectedMessageCount() {
        return expectedMessageCount;
    }

    public RouteDefinition getMockFeederRoute() {
        return mockFeederRoute;
    }

    public Predicate getLenientSelector() {
        return lenientSelector;
    }

    public LenientProcessor getLenientProcessor() {
        return lenientProcessor;
    }

    public long getAssertionTime() {
        return assertionTime;
    }

    public static class MockDefinitionBuilder<Builder extends MockDefinitionBuilder<Builder>> {

        private String endpointUri;
        private OrderingType orderingType = OrderingType.TOTAL;
        private boolean isEndpointOrdered = true;
        private Predicate lenientSelector = null;
        private List<List<Processor>> partProcessors = new ArrayList<>();
        private List<List<Predicate>> partPredicates = new ArrayList<>();
        private int expectedMessageCount = 1;
        private RouteDefinition mockFeederRoute = null;
        private Class<? extends LenientProcessor> lenientProcessorClass = LenientProcessor.class;
        private LenientProcessor lenientProcessor;

        private List<Processor> repeatedProcessors = new ArrayList<>();
        private List<Predicate> repeatedPredicates = new ArrayList<>();

        //final list of processors/predicates after build
        private List<Processor> processors = new ArrayList<>();
        private List<Predicate> predicates = new ArrayList<>();

        private long assertionTime = 15000l;

        /**
         * @param endpointUri A Camel Endpoint URI to listen to for expected messages
         */
        public MockDefinitionBuilder(String endpointUri) {
            try {
                this.endpointUri = URISupport.normalizeUri(endpointUri);
            } catch (URISyntaxException | UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }

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

        public Builder lenient() {
            return lenient(new Predicate() {
                @Override
                public boolean matches(Exchange exchange) {
                    return true;
                }
            });
        }

        public Builder lenient(Predicate lenientSelector) {
            this.lenientSelector = lenientSelector;
            return self();
        }

        public Builder lenientProcessor(Class<? extends LenientProcessor> lenientProcessorClass) {
            this.lenientProcessorClass = lenientProcessorClass;
            return self();
        }

        public Builder addProcessors(Processor... processors) {
            this.partProcessors.add(Arrays.asList(processors));
            return self();
        }

        public Builder addProcessors(int index, Processor... processors) {
            while (index > this.partProcessors.size()) {
                this.partProcessors.add(new ArrayList<Processor>());
            }
            this.partProcessors.get(index).addAll(Arrays.asList(processors));
            return self();
        }

        public Builder addRepeatedProcessor(Processor processor) {
            repeatedProcessors.add(processor);
            return self();
        }

        public Builder addPredicates(Predicate... predicates) {
            this.partPredicates.add(Arrays.asList(predicates));
            return self();
        }

        public Builder addPredicates(int index, Predicate... predicates) {
            while (index > this.partPredicates.size()) {
                this.partPredicates.add(new ArrayList<Predicate>());
            }
            this.partPredicates.get(index).addAll(Arrays.asList(predicates));
            return self();
        }

        public Builder addRepeatedPredicate(Predicate predicate) {
            repeatedPredicates.add(predicate);
            return self();
        }

        public Builder mockFeederRoute(RouteDefinition mockFeederRoute) {
            this.mockFeederRoute = mockFeederRoute;
            return self();
        }

        public Builder assertionTime(long assertionTime) {
            this.assertionTime = assertionTime;
            return self();
        }

        public MockDefinition build(MockDefinition previousDefinitionPart) {

            if (expectedMessageCount < 0)
                throw new IllegalStateException("The expected message count for the mock definition on endpoint "
                        + endpointUri + " must be at least 0");

            expectedMessageCount = Math.max(expectedMessageCount, partPredicates.size());

            if (lenientSelector != null && previousDefinitionPart.lenientSelector != null)
                throw new IllegalStateException(endpointUri + " can have only one part of a mock endpoint defined as lenient");

            if (lenientSelector != null) {
                if (expectedMessageCount > 0)
                    logger.warn("Expectations for a lenient endpoint part {} will be ignored", endpointUri);

                expectedMessageCount = 0;
                partPredicates.clear();

                List<Processor> lenientProcessorList = new ArrayList<>();
                for (List<Processor> processorList : partProcessors) {
                    List<Processor> processorListCopy = new ArrayList<>(processorList);
                    processorListCopy.addAll(repeatedProcessors);
                    lenientProcessorList.add(new MultiProcessor(Collections.unmodifiableList(processorListCopy)));
                }

                try {
                    lenientProcessor = lenientProcessorClass.getDeclaredConstructor(List.class)
                            .newInstance(lenientProcessorList);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                        | NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }

            if (partPredicates.size() < expectedMessageCount)
                logger.warn("The endpoint {} has fewer part predicates provided than the expected message count", endpointUri);

            Processor[] repeatedProcessorsArray = repeatedProcessors.toArray(new Processor[repeatedProcessors.size()]);
            Predicate[] repeatedPredicatesArray = repeatedPredicates.toArray(new Predicate[repeatedPredicates.size()]);

            //do repeated partPredicates and repeated expectations (this will also pad out partProcessors and partPredicates up
            //expected message count)
            for (int i = 0; i < expectedMessageCount; i++) {
                //note the lenient repeated processors are added above
                addProcessors(i, repeatedProcessorsArray);
                addPredicates(i, repeatedPredicatesArray);

                //the final single processor/predicates lists
                processors.add(new MultiProcessor(partProcessors.get(i)));
                predicates.add(new MultiPredicate(partPredicates.get(i)));
            }

            //getProcessors - for each processor, combine list, add repeated processor and return

            //ensure the number of partProcessors doesn't mess up the next expectation
            if (partProcessors.size() > expectedMessageCount && lenientSelector != null) {
                logger.warn("The mock definition endpoint {} has {} expected messages but only {} message " +
                        "part processors; the additional processors will be ignored to match the expected message count",
                        new Object[]{endpointUri, partPredicates.size(), partProcessors.size()});
            }

            if (previousDefinitionPart == null) {
                //set up a default expectation feeder route (sending to a mock will be added later)
                if (mockFeederRoute == null) mockFeederRoute = new RouteDefinition().convertBodyTo(String.class);
                mockFeederRoute.from(endpointUri);
            }

            if (previousDefinitionPart != null) {
                if (!previousDefinitionPart.getEndpointUri().equals(endpointUri))
                    throw new IllegalStateException("The endpoints do not much for merging mock definition endpoint " +
                            previousDefinitionPart.getEndpointUri() + " with endpoint " + endpointUri);

                if (previousDefinitionPart.isEndpointOrdered() != isEndpointOrdered)
                    throw new IllegalStateException("The endpoint ordering must be the same for all mock definition parts of " +
                            "endpoint " + endpointUri);

                if (previousDefinitionPart.getOrderingType() != orderingType)
                    throw new IllegalStateException("The ordering type must be same for all mock definition parts on the endpoint "
                            + endpointUri);

                if (mockFeederRoute != null)
                    throw new IllegalStateException("The mock feeder route for the endpoint " + endpointUri +
                            " can only be specified in the first mock definition part");

                if (previousDefinitionPart.getAssertionTime() != assertionTime) {
                    logger.warn("The assertion time for a subsequent mock definition part on endpoint {} has a different " +
                            "assertion time - the first will be used and will apply to the endpoint as a whole",endpointUri);
                    this.assertionTime = previousDefinitionPart.getAssertionTime();
                }

                //prepend the previous partPredicates/partProcessors onto this list ot make an updated expectation
                this.predicates.addAll(0, previousDefinitionPart.getPredicates());
                this.processors.addAll(0, previousDefinitionPart.getProcessors());
                this.expectedMessageCount += previousDefinitionPart.getExpectedMessageCount();
                this.mockFeederRoute = previousDefinitionPart.getMockFeederRoute();
                this.isEndpointOrdered = previousDefinitionPart.isEndpointOrdered;
                //we know if this isn't null then we have checked this expectation hasn't set a selector/processor
                if (previousDefinitionPart.lenientSelector != null) {
                    this.lenientSelector = previousDefinitionPart.lenientSelector;
                    this.lenientProcessor = previousDefinitionPart.lenientProcessor;
                }
            }

            return new MockDefinition(this);
        }

        protected Builder self() {
            return (Builder) this;
        }

        public String getEndpointUri() {
            return this.endpointUri;
        }

        protected List<List<Predicate>> getPartPredicates() {
            return Collections.unmodifiableList(partPredicates);
        }

        protected List<List<Processor>> getPartProcessors() {
            return Collections.unmodifiableList(partProcessors);
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
    private MockDefinition(MockDefinitionBuilder builder) {

        this.endpointUri = builder.endpointUri;
        this.orderingType = builder.orderingType;
        this.isEndpointOrdered = builder.isEndpointOrdered;
        this.processors = builder.processors;
        this.predicates = builder.predicates;
        this.expectedMessageCount = builder.expectedMessageCount;
        this.lenientProcessor = builder.lenientProcessor;
        this.lenientSelector = builder.lenientSelector;
        this.assertionTime = builder.assertionTime;

        this.mockFeederRoute = builder.mockFeederRoute;
    }
}
