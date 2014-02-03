package nz.ac.auckland.integration.testing.specification;

import nz.ac.auckland.integration.testing.MorcBuilder;
import nz.ac.auckland.integration.testing.endpointoverride.EndpointOverride;
import nz.ac.auckland.integration.testing.mock.MockDefinition;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * An orchestrated test is a declarative way of specifying a target for a message with a number
 * of expectations related to call-outs required for the process that the original message triggered
 * is deemed to be correct.
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class OrchestratedTestSpecification {
    private static final Logger logger = LoggerFactory.getLogger(OrchestratedTestSpecification.class);
    private String description;
    private String endpointUri;
    private Collection<MockDefinition> mockDefinitions;
    private long assertTime;
    private Collection<EndpointOverride> endpointOverrides = new ArrayList<>();
    private Collection<EndpointNode> endpointNodesOrdering;
    private long sendInterval;
    private int partCount;
    private OrchestratedTestSpecification nextPart;
    private List<Processor> processors;
    private List<Predicate> predicates;
    private int totalMessageCount;

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
     * @return The amount of time in milliseconds that the test should wait before validating all expectations.
     *         Only applies with asynchronous/partially ordered/unreceived expectations
     */
    public long getAssertTime() {
        return assertTime;
    }

    /**
     * @return The interval in milliseconds between sending multiple messages
     */
    public long getSendInterval() {
        return sendInterval;
    }

    public String getEndpointUri() {
        return endpointUri;
    }

    public List<Predicate> getPredicates() {
        return predicates;
    }

    public List<Processor> getProcessors() {
        return processors;
    }

    public Collection<EndpointOverride> getEndpointOverrides() {
        return Collections.unmodifiableCollection(endpointOverrides);
    }

    public int getTotalMessageCount() {
        return totalMessageCount;
    }

    //Builder/DSL/Fluent API inheritance has been inspired by the blog: https://weblogs.java.net/node/642849
    public static class OrchestratedTestSpecificationBuilder<Builder extends MorcBuilder<Builder>> extends MorcBuilder<Builder> {

        private String description;
        private Map<String, MockDefinition> mockExpectations = new HashMap<>();
        private long assertTime = 15000l;
        private long sendInterval = 1000l;
        private int partCount = 1;
        private OrchestratedTestSpecification nextPart = null;
        //todo: add time to wait for all requests to be sent
        private Collection<EndpointNode> endpointNodesOrdering = new ArrayList<>();
        private EndpointNode currentTotalOrderLeafEndpoint;
        private int totalMessageCount = 0;

        //final list of single processors and predicates
        private List<Processor> processors;
        private List<Predicate> predicates;

        private OrchestratedTestSpecificationBuilder nextPartBuilder;

        public OrchestratedTestSpecificationBuilder(String description, String endpointUri) {
            super(endpointUri);
            this.description = description;
        }

        public OrchestratedTestSpecification build() {
            if (nextPartBuilder != null) {
                nextPart = nextPartBuilder.build();
                partCount = nextPart.getPartCount() + 1;
            }

            processors = getProcessors();
            if (processors.size() == 0) throw new IllegalStateException("The specification for endpoint " + getEndpointUri() +
                    " must specify at least one message processor to send messages");
            predicates = getPredicates();
            return new OrchestratedTestSpecification(this);
        }

        /**
         * We use an EndpointSubscriber Builder here as we have to do additional configuration before creation. This includes
         * setting up the expected index that an expectation is received which means that previous calls to
         * MockDefinition.receivedAt() will be ignored.
         *
         * @param mockDefinitionBuilder The expectation builder used to seed the expectation
         * @throws IllegalArgumentException if expectations to the same endpoint have different ordering requirements
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

            totalMessageCount += mergedEndpointExpectationMessageCount;

            //we need to build a tree based on ordering types which will be expanded to a set during validation

            //endpoints with no relative ordering are always in the base set
            if (mergedExpectation.getOrderingType() == MockDefinition.OrderingType.NONE) {
                //these will always be in the accepted set
                for (int i = 0; i < mergedEndpointExpectationMessageCount; i++)
                    endpointNodesOrdering.add(new EndpointNode(mergedExpectation.getEndpointUri()));
            }

            //endpoints partially ordered to other endpoints will be added to the set after they are encountered
            //by a totally ordered endpoint unless they occur at the start of an expectation builder
            if (mergedExpectation.getOrderingType() == MockDefinition.OrderingType.PARTIAL) {
                for (int i = 0; i < mergedEndpointExpectationMessageCount; i++) {
                    if (currentTotalOrderLeafEndpoint == null)
                        endpointNodesOrdering.add(new EndpointNode(mergedExpectation.getEndpointUri()));
                    else
                        currentTotalOrderLeafEndpoint.childrenNodes.add(new EndpointNode(mergedExpectation.getEndpointUri()));
                }
            }

            //only TOTAL ordered can have children and will create order (a tree structure) which is added to the set
            //on endpoint ordering matches
            if (mergedExpectation.getOrderingType() == MockDefinition.OrderingType.TOTAL) {
                for (int i = 0; i < mergedEndpointExpectationMessageCount; i++) {
                    EndpointNode nextTotalOrderedNode = new EndpointNode(mergedExpectation.getEndpointUri());
                    if (currentTotalOrderLeafEndpoint == null) endpointNodesOrdering.add(nextTotalOrderedNode);
                    else currentTotalOrderLeafEndpoint.childrenNodes.add(nextTotalOrderedNode);

                    currentTotalOrderLeafEndpoint = nextTotalOrderedNode;
                }
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
         * @param sleepForTestCompletion The amount of time in milliseconds that the test should wait before
         *                               validating all expectations. Only applies with asynchronous/partially
         *                               ordered/unreceived expectations
         */
        public Builder sleepForTestCompletion(long sleepForTestCompletion) {
            this.assertTime = sleepForTestCompletion;
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

        @SuppressWarnings("unchecked")
        public Builder addEndpoint(String endpointUri) {
            return (Builder)addEndpoint(endpointUri, this.getClass());
        }

        @SuppressWarnings("unchecked")
        public <T extends OrchestratedTestSpecificationBuilder<?>> T addEndpoint(String endpointUri, Class<T> clazz) {
            try {
                this.nextPartBuilder = clazz.getDeclaredConstructor(String.class, String.class)
                        .newInstance(description, endpointUri);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                    | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
            return (T) this.nextPartBuilder;
        }

        protected Map<String, MockDefinition> getMockExpectations() {
            return Collections.unmodifiableMap(mockExpectations);
        }

    }

    @SuppressWarnings("unchecked")
    private OrchestratedTestSpecification(OrchestratedTestSpecificationBuilder builder) {
        this.description = builder.description;
        this.endpointUri = builder.getEndpointUri();
        this.mockDefinitions = builder.mockExpectations.values();
        this.assertTime = builder.assertTime;
        this.endpointOverrides = builder.getEndpointOverrides();
        this.sendInterval = builder.sendInterval;
        this.partCount = builder.partCount;
        this.nextPart = builder.nextPart;
        this.endpointNodesOrdering = builder.endpointNodesOrdering;
        this.processors = builder.processors;
        this.predicates = builder.predicates;
        this.totalMessageCount = builder.totalMessageCount;
    }

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

