package nz.ac.auckland.integration.testing.mock;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.util.URISupport;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.*;
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
    private RouteDefinition expectationFeederRoute;
    private Predicate lenientSelector;
    private Processor lenientProcessor;

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

    public RouteDefinition getExpectationFeederRoute() {
        return expectationFeederRoute;
    }

    public Predicate getLenientSelector() {
        return lenientSelector;
    }


    /*
        Using details from: https://weblogs.java.net/node/642849
     */
    public static class MockDefinitionBuilder<Builder extends MockDefinitionBuilder<Builder>> {

        private String endpointUri;
        private OrderingType orderingType = OrderingType.TOTAL;
        private boolean isEndpointOrdered = true;
        private Predicate lenientSelector = null;
        private List<List<Processor>> processors = new ArrayList<>();
        private List<List<Predicate>> predicates = new ArrayList<>();
        private int expectedMessageCount = 1;
        private RouteDefinition expectationFeederRoute = null;
        private Class<? extends DefaultProcessor> lenientProcessorClass = DefaultProcessor.class;

        private List<Processor> repeatedProcessors = new ArrayList<>();
        private List<Predicate> repeatedPredicates = new ArrayList<>();

        private long assertPeriod = 15000l;

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

        public Builder addProcessors(Processor... processors) {
            this.processors.add(Arrays.asList(processors));
            return self();
        }

        public Builder lenientProcessor(Class<? extends DefaultProcessor> lenientProcessorClass) {
            this.lenientProcessorClass = lenientProcessorClass;
            return self();
        }

        public Builder addProcessors(int index, Processor... processors) {
            while (index > this.processors.size()) {
                this.processors.add(new ArrayList<Processor>());
            }
            this.processors.get(index).addAll(Arrays.asList(processors));
            return self();
        }

        public Builder addRepeatedProcessor(Processor processor) {
            repeatedProcessors.add(processor);
            return self();
        }

        public Builder addPredicates(Predicate... predicates) {
            this.predicates.add(Arrays.asList(predicates));
            return self();
        }

        public Builder addPredicates(int index, Predicate... predicates) {
            while (index > this.predicates.size()) {
                this.predicates.add(new ArrayList<Predicate>());
            }
            this.predicates.get(index).addAll(Arrays.asList(predicates));
            return self();
        }

        public Builder addRepeatedPredicate(Predicate predicate) {
            repeatedPredicates.add(predicate);
            return self();
        }

        public Builder expectationFeederRoute(RouteDefinition expectationFeederRoute) {
            this.expectationFeederRoute = expectationFeederRoute;
            return self();
        }

        public MockDefinition build(MockDefinition previousExpectationPart) {

            if (expectedMessageCount < 0)
                throw new IllegalStateException("The expected message count for the expectation on endpoint "
                        + endpointUri + " must be at least 0");

            expectedMessageCount = Math.max(expectedMessageCount, predicates.size());

            if (lenientSelector != null && previousExpectationPart.lenientSelector != null)
                throw new IllegalStateException("endpoint .. lenient selector");

            if (lenientSelector != null) {
                if (expectedMessageCount > 0)
                    logger.warn("");

                expectedMessageCount = 0;
                predicates.clear();
            }

            if (predicates.size() < expectedMessageCount)
                logger.warn("...");

            Processor[] repeatedProcessorsArray = repeatedProcessors.toArray(new Processor[repeatedProcessors.size()]);
            Predicate[] repeatedPredicatesArray = repeatedPredicates.toArray(new Predicate[repeatedPredicates.size()]);

            //do repeated predicates and repeated expectations (this will also pad out processors and predicates up
            //expected message count)
            for (int i = 0; i < expectedMessageCount; i++) {
                addProcessors(i,repeatedProcessorsArray);
                addPredicates(i,repeatedPredicatesArray);
            }

            //ensure the number of processors doesn't mess up the next expectation
            if (processors.size() > expectedMessageCount) {
                logger.warn("The endpoint {} has {} expected messages but only {} message processors",
                        new Object[] {endpointUri,predicates.size(),processors.size()});

                //what about lenient? add to lenient processor


                logger.warn("The additional processors for expectation endpoint {} are being removed to match the " +
                        "expected message count",endpointUri);
                while (processors.size() > expectedMessageCount) {
                    List<Processor> removedProcessors = processors.remove(processors.size()-1);
                    logger.debug("The endpoint {} is having the expectation processors removed: {}",
                            endpointUri,StringUtils.join(removedProcessors,","));
                }

            }

            List<Processor> singleProcessorList = new ArrayList<>();
            List<Predicate> singlePredicateList = new ArrayList<>();

            //we know number of processors/predicates is same size as expectedMessageCount
            for (int i = 0; i < expectedMessageCount; i++) {
                singleProcessorList.add(new DefaultProcessor(processors.get(i)));
                singlePredicateList.add(new DefaultPredicate(predicates.get(i)));
            }

            if (previousExpectationPart == null && !isEndpointOrdered) {
                //create internal list predicate
            }

            if (previousExpectationPart != null) {

                if (!previousExpectationPart.getEndpointUri().equals(endpointUri))
                    throw new IllegalStateException("The endpoint do not much for merging expectation " +
                            previousExpectationPart.getEndpointUri() + " and " + endpointUri);

                //this isn't really necessary for sync ordering...
                if (previousExpectationPart.isEndpointOrdered() != isEndpointOrdered)
                    throw new IllegalStateException("The endpoint ordering must be the same for all expectation parts of " +
                            "endpoint " + endpointUri);

                if (previousExpectationPart.getOrderingType() != orderingType)
                    throw new IllegalStateException("The ordering type must be same for all expectations on an endpoint: "
                            + endpointUri);

                if (expectationFeederRoute != null)
                    throw new IllegalStateException("The expectation feeder route for the endpoint " + endpointUri +
                                            " can only be specified in the first expectation part");

                if (!isEndpointOrdered) {
                    //if (predicates != null)
                    //  cast predicates.get(0)
                    //  if not null add new predicates to internal list
                    //else
                    //  create internal list predicate
                }

                //prepend the previous predicates/processors onto this list ot make an updated expectation
                this.predicates.addAll(0, previousExpectationPart.getPredicates());
                this.processors.addAll(0, previousExpectationPart.getProcessors());
                this.expectedMessageCount += previousExpectationPart.getExpectedMessageCount();
                this.expectationFeederRoute = previousExpectationPart.getExpectationFeederRoute();
            }

            return new MockDefinition(this);
        }

        protected Builder self() {
            return (Builder)this;
        }

        public String getEndpointUri() {
            return this.endpointUri;
        }

        protected List<List<Predicate>> getPredicates() {
            return Collections.unmodifiableList(predicates);
        }

        protected List<List<Processor>> getProcessors() {
            return Collections.unmodifiableList(processors);
        }

        protected OrderingType getOrderingType() {
            return this.orderingType;
        }
    }

    public static class DefaultProcessor implements Processor {

        protected List<Processor> processors;

        public DefaultProcessor(List<Processor> processors) {
            this.processors = processors;
        }

        @Override
        public void process(Exchange exchange) throws Exception {
            for (Processor processor : processors) {
                processor.process(exchange);
            }
        }
    }

    public static class DefaultPredicate implements Predicate {
        private List<Predicate> predicates;

        public DefaultPredicate(List<Predicate> predicates) {
            this.predicates = predicates;
        }

        @Override
        public boolean matches(Exchange exchange) {
            for (Predicate predicate : predicates) {
                if (!predicate.matches(exchange)) return false;
            }
            return true;
        }
    }

    @SuppressWarnings("unchecked")
    protected MockDefinition(MockDefinitionBuilder builder) {
        this.endpointUri = builder.endpointUri;
        this.orderingType = builder.orderingType;
        this.isEndpointOrdered = builder.isEndpointOrdered;
        this.processors = builder.processors;
        this.predicates = builder.predicates;
        this.expectedMessageCount = builder.expectedMessageCount;


        //if !endpointOrdering then will have to wrap

        this.expectationFeederRoute = builder.expectationFeederRoute;

        //set up a default expectation feeder route
        if (expectationFeederRoute == null) expectationFeederRoute = new RouteDefinition().convertBodyTo(String.class);
        //todo: ensure we only add the feed the first time
        expectationFeederRoute.from(endpointUri);
    }
}
