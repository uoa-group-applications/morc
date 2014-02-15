package nz.ac.auckland.integration.testing.specification;

import nz.ac.auckland.integration.testing.MorcBuilder;
import nz.ac.auckland.integration.testing.endpointoverride.EndpointOverride;
import nz.ac.auckland.integration.testing.mock.MockDefinition;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * An orchestrated test is a declarative way of specifying a target for a message with a number
 * of expectations related to call-outs required for the process that the original message triggered
 * is deemed to be correct. A DSL builder is used to generate an instate of an orchestrated test specification
 * which is executed as part of a MorcTest
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class OrchestratedTestSpecification {
    private static final Logger logger = LoggerFactory.getLogger(OrchestratedTestSpecification.class);
    private String description;
    private String endpointUri;
    private Collection<MockDefinition> mockDefinitions;
    private long messageResultWaitTime;
    private Collection<EndpointOverride> endpointOverrides = new ArrayList<>();
    private Collection<EndpointNode> endpointNodesOrdering;
    private long sendInterval;
    private int partCount;
    private OrchestratedTestSpecification nextPart;
    private List<Processor> processors;
    private List<Predicate> predicates;
    private int totalMockMessageCount;
    private long minimalResultWaitTime;

    /**
     * @return A description that explains what this tests is doing
     */
    public String getDescription() {
        return description;
    }

    public Collection<EndpointNode> getEndpointNodesOrdering() {
        return Collections.unmodifiableCollection(endpointNodesOrdering);
    }

    /**
     * @return The number of parts involved in this specification, where each part is a specification in itself
     */
    public int getPartCount() {
        return partCount;
    }

    /**
     * @return The next specification in the line for this test (recursive)
     */
    public OrchestratedTestSpecification getNextPart() {
        return nextPart;
    }

    /**
     * @return A list of expectations that need to be satisfied for the test to pass
     */
    public Collection<MockDefinition> getMockDefinitions() {
        return Collections.unmodifiableCollection(mockDefinitions);
    }

    /**
     * @return The amount of time in milliseconds that the test will wait for all responses to be received back from
     *         the target endpoint URI
     */
    public long getResultWaitTime() {
        //10s gives time for the route to get booted
        return minimalResultWaitTime + (messageResultWaitTime * getTotalPublishMessageCount());
    }

    /**
     * @return The interval in milliseconds between sending multiple messages
     */
    public long getSendInterval() {
        return sendInterval;
    }

    /**
     * @return The endpoint URI of the target service under testing
     */
    public String getEndpointUri() {
        return endpointUri;
    }

    /**
     * @return The list of predicates that will be used to validate any responses back from the call out
     */
    public List<Predicate> getPredicates() {
        return predicates;
    }

    /**
     * @return The list of processors that will populate the message exchange before sending to the target endpoint
     */
    public List<Processor> getProcessors() {
        return processors;
    }

    /**
     * @return The set of overrides that will modify the definition's endpoint
     */
    public Collection<EndpointOverride> getEndpointOverrides() {
        return Collections.unmodifiableCollection(endpointOverrides);
    }

    /**
     * @return The total number of messages that the mock definitions/expectations expect to receive
     */
    public int getTotalMockMessageCount() {
        return totalMockMessageCount;
    }

    /**
     * @return The total number of messages that will be sent to the target endpoint
     */
    public int getTotalPublishMessageCount() {
        return processors.size();
    }

    public static class OrchestratedTestSpecificationBuilder extends OrchestratedTestSpecificationBuilderInit<OrchestratedTestSpecificationBuilder> {
        public OrchestratedTestSpecificationBuilder(String description, String endpointUri) {
            super(description,endpointUri);
        }
    }

    //Builder/DSL/Fluent API inheritance has been inspired by the blog: https://weblogs.java.net/node/642849
    public static class OrchestratedTestSpecificationBuilderInit<Builder extends MorcBuilder<Builder>> extends MorcBuilder<Builder> {

        private String description;
        private Map<String, MockDefinition> mockExpectations = new HashMap<>();
        private long sendInterval = 1000l;
        private int partCount = 1;
        private OrchestratedTestSpecification nextPart = null;
        private Collection<EndpointNode> endpointNodesOrdering = new ArrayList<>();
        private EndpointNode currentTotalOrderLeafEndpoint;
        private int totalMockMessageCount = 0;
        private boolean expectsException = false;

        //final list of single processors and predicates
        private List<Processor> processors;
        private List<Predicate> predicates;

        private StringBuilder endpointOrderingStringBuilder = new StringBuilder();

        private OrchestratedTestSpecificationBuilderInit previousPartBuilder;

        /**
         * @param description The description that identifies what the test is supposed to do
         * @param endpointUri The endpoint URI of the target service under testing
         */
        public OrchestratedTestSpecificationBuilderInit(String description, String endpointUri) {
            super(endpointUri);
            this.description = description;
        }

        protected OrchestratedTestSpecificationBuilderInit(String description, String endpointUri, OrchestratedTestSpecificationBuilderInit previousPartBuilder) {
            this(description,endpointUri);
            this.previousPartBuilder = previousPartBuilder;
        }

        public final OrchestratedTestSpecification build() {
            return build(1,null);
        }

        protected OrchestratedTestSpecification build(int partCount, OrchestratedTestSpecification nextPart) {
            if (!expectsException)
                addRepeatedPredicate(new Predicate() {
                    @Override
                    public boolean matches(Exchange exchange) {
                        return exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class) == null;
                    }

                    @Override
                    public String toString() {
                        return "UnexpectedExceptionPredicate";
                    }
                });
            else
                addRepeatedPredicate(new Predicate() {
                    @Override
                    public boolean matches(Exchange exchange) {
                        return exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class) != null;
                    }

                    @Override
                    public String toString() {
                        return "ExpectedExceptionPredicate";
                    }
                });

            processors = getProcessors();
            if (processors.size() == 0) throw new IllegalArgumentException("The specification for test " + description +
                    " on endpoint " + getEndpointUri() + " must specify at least one message processor to send messages");

            predicates = getPredicates(processors.size());

            logger.info("The test {} on endpoint {} will be sending {} messages and validate {} responses to endpoint {}",
                    new Object[]{description, getEndpointUri(),processors.size(), predicates.size(), getEndpointUri()});

            logger.debug("The test {} on endpoint {} will have the following expectation ordering {}",
                    new Object[] {description,getEndpointUri(),endpointOrderingStringBuilder.toString()});

            this.partCount = partCount;
            this.nextPart = nextPart;

            if (previousPartBuilder != null) {
                return previousPartBuilder.build(partCount+1,new OrchestratedTestSpecification(this));
            } else
                return new OrchestratedTestSpecification(this);
        }

        /**
         * @param mockDefinitionBuilder The expectation builder used to seed the expectation
         */
        public Builder addExpectation(MockDefinition.MockDefinitionBuilderInit mockDefinitionBuilder) {

            //we need to merge the expectations
            MockDefinition endpointExpectation = mockExpectations.get(mockDefinitionBuilder.getEndpointUri());

            int currentEndpointExpectationMessageCount = 0;

            if (endpointExpectation != null)
                currentEndpointExpectationMessageCount = endpointExpectation.getExpectedMessageCount();

            MockDefinition mergedExpectation =
                    mockDefinitionBuilder.build(endpointExpectation);

            int mergedEndpointExpectationMessageCount =
                    mergedExpectation.getExpectedMessageCount() - currentEndpointExpectationMessageCount;

            logger.trace("Adding {} expected messages to expectation URI {} on test {}", new Object[]{
                    mergedEndpointExpectationMessageCount, mockDefinitionBuilder.getEndpointUri(), description});

            totalMockMessageCount += mergedEndpointExpectationMessageCount;

            //we need to build a tree based on ordering types which will be expanded to a set during validation

            //endpoints with no relative ordering are always in the base set
            if (mergedExpectation.getOrderingType() == MockDefinition.OrderingType.NONE) {
                //these will always be in the accepted set
                StringBuilder noneStringBuilder = new StringBuilder("NONE: (");
                for (int i = 0; i < mergedEndpointExpectationMessageCount; i++) {
                    endpointNodesOrdering.add(new EndpointNode(mergedExpectation.getEndpointUri()));
                    noneStringBuilder.append(mergedExpectation.getEndpointUri());
                    if (i < mergedEndpointExpectationMessageCount - 1) noneStringBuilder.append(",");
                }
                //this should be at the start!
                noneStringBuilder.append(") ");
                endpointOrderingStringBuilder = new StringBuilder(noneStringBuilder.toString() + endpointOrderingStringBuilder.toString());

            }

            //endpoints partially ordered to other endpoints will be added to the set after they are encountered
            //by a totally ordered endpoint unless they occur at the start of an expectation builder
            if (mergedExpectation.getOrderingType() == MockDefinition.OrderingType.PARTIAL) {
                if (endpointOrderingStringBuilder.length() != 0) endpointOrderingStringBuilder.append(" -> ");
                endpointOrderingStringBuilder.append("PARTIAL: (");

                for (int i = 0; i < mergedEndpointExpectationMessageCount; i++) {
                    if (currentTotalOrderLeafEndpoint == null)
                        endpointNodesOrdering.add(new EndpointNode(mergedExpectation.getEndpointUri()));
                    else
                        currentTotalOrderLeafEndpoint.childrenNodes.add(new EndpointNode(mergedExpectation.getEndpointUri()));

                    endpointOrderingStringBuilder.append(mergedExpectation.getEndpointUri());
                    if (i < mergedEndpointExpectationMessageCount - 1) endpointOrderingStringBuilder.append(",");
                }

                endpointOrderingStringBuilder.append(")");
            }

            //only TOTAL ordered can have children and will create order (a tree structure) which is added to the set
            //on endpoint ordering matches
            if (mergedExpectation.getOrderingType() == MockDefinition.OrderingType.TOTAL) {
                if (endpointOrderingStringBuilder.length() != 0) endpointOrderingStringBuilder.append(" -> ");
                endpointOrderingStringBuilder.append("TOTAL: (");

                for (int i = 0; i < mergedEndpointExpectationMessageCount; i++) {
                    EndpointNode nextTotalOrderedNode = new EndpointNode(mergedExpectation.getEndpointUri());
                    if (currentTotalOrderLeafEndpoint == null) endpointNodesOrdering.add(nextTotalOrderedNode);
                    else currentTotalOrderLeafEndpoint.childrenNodes.add(nextTotalOrderedNode);

                    currentTotalOrderLeafEndpoint = nextTotalOrderedNode;

                    endpointOrderingStringBuilder.append(mergedExpectation.getEndpointUri());
                    if (i < mergedEndpointExpectationMessageCount - 1) endpointOrderingStringBuilder.append(",");
                }

                endpointOrderingStringBuilder.append(")");
            }

            mockExpectations.put(mockDefinitionBuilder.getEndpointUri(), mergedExpectation);

            return self();
        }

        /**
         * A convenience method for adding multiple expectations at the same time
         */
        public Builder addExpectations(MockDefinition.MockDefinitionBuilderInit... expectationBuilders) {
            for (MockDefinition.MockDefinitionBuilderInit expectationBuilder : expectationBuilders) {
                addExpectation(expectationBuilder);
            }

            return self();
        }

        /**
         * @param sendInterval The interval in milliseconds between sending multiple messages, defaults to 1000ms
         */
        public Builder sendInterval(long sendInterval) {
            if (sendInterval <= 0)
                throw new IllegalArgumentException("You must specify an interval > 0 between sending messages");
            this.sendInterval = sendInterval;
            return self();
        }

        /**
         * @param endpointUri Specify an additional endpoint to call after this part of the test specification has
         *                    completed successfully
         */
        @SuppressWarnings("unchecked")
        public Builder addEndpoint(String endpointUri) {
            return (Builder) addEndpoint(endpointUri, this.getClass());
        }

        /**
         * Specifies that the request to the endpoint URI will result in an exception being returned
         */
        public Builder expectsException() {
            this.expectsException = true;
            return self();
        }

        /**
         * @param endpointUri Specify an additional endpoint to call after this part of the test specification has
         *                    completed successfully
         * @param clazz       The type of builder that will be used for the next part of the specification
         */
        public <T extends OrchestratedTestSpecificationBuilderInit<?>> T addEndpoint(String endpointUri, Class<T> clazz) {
            try {
                return clazz.getDeclaredConstructor(String.class, String.class,OrchestratedTestSpecificationBuilderInit.class)
                        .newInstance(description, endpointUri, this);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                    | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        protected Map<String, MockDefinition> getMockExpectations() {
            return Collections.unmodifiableMap(mockExpectations);
        }

        protected String getDescription() {
            return this.description;
        }

    }

    @SuppressWarnings("unchecked")
    private OrchestratedTestSpecification(OrchestratedTestSpecificationBuilderInit builder) {
        this.description = builder.description;
        this.endpointUri = builder.getEndpointUri();
        this.mockDefinitions = builder.mockExpectations.values();
        this.messageResultWaitTime = Math.max(builder.getMessageResultWaitTime(), builder.sendInterval);
        this.endpointOverrides = builder.getEndpointOverrides();
        this.sendInterval = builder.sendInterval;
        this.partCount = builder.partCount;
        this.nextPart = builder.nextPart;
        this.endpointNodesOrdering = builder.endpointNodesOrdering;
        this.processors = builder.processors;
        this.predicates = builder.predicates;
        this.totalMockMessageCount = builder.totalMockMessageCount;
        this.minimalResultWaitTime = builder.getMinimalResultWaitTime();
    }

    /**
     * A convenience class that allows us to specify ordering between the expectations
     */
    public static class EndpointNode {
        private String endpointUri;
        private Collection<EndpointNode> childrenNodes = new ArrayList<>();

        public EndpointNode(String endpointUri) {
            this.endpointUri = endpointUri;
        }

        public Collection<EndpointNode> getChildrenNodes() {
            return Collections.unmodifiableCollection(childrenNodes);
        }

        public String getEndpointUri() {
            return this.endpointUri;
        }
    }
}

