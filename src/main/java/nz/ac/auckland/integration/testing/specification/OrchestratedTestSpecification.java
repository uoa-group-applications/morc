package nz.ac.auckland.integration.testing.specification;

import nz.ac.auckland.integration.testing.MorcBuilder;
import nz.ac.auckland.integration.testing.OrchestratedTest;
import nz.ac.auckland.integration.testing.endpointoverride.CxfEndpointOverride;
import nz.ac.auckland.integration.testing.endpointoverride.EndpointOverride;
import nz.ac.auckland.integration.testing.endpointoverride.UrlConnectionOverride;
import nz.ac.auckland.integration.testing.mock.MockDefinition;
import org.apache.camel.*;
import org.apache.camel.util.URISupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
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
    private final Set<MockDefinition> mockDefinitions;
    private long assertTime;
    private Collection<EndpointOverride> endpointOverrides = new ArrayList<>();
    private Queue<MockDefinition> endpointOrdering;
    private int sendCount;
    private long sendInterval;
    private int partCount;
    private OrchestratedTestSpecification nextPart;
    private List<Processor> processors;
    private List<Predicate> predicates;

    /**
     * @return A description that explains what this tests is doing
     */
    public String getDescription() {
        return description;
    }

    public Queue<MockDefinition> getEndpointOrdering() {
        return this.endpointOrdering;
    }

    /**
     * @return The number of parts involved in this specification, where each part is a specification in itself
     */
    public int getPartCount() {
        return this.partCount;
    }

    /**
     * @return The next specification in the line for this test (recursive)
     */
    public OrchestratedTestSpecification getNextPart() {
        return this.nextPart;
    }

    /**
     * @return A list of expectations that need to be satisfied for the test to pass
     */
    public Set<MockDefinition> getMockDefinitions() {
        return Collections.unmodifiableSet(mockDefinitions);
    }

    /**
     * @return The amount of time in milliseconds that the test should wait before validating all expectations.
     *         Only applies with asynchronous/partially ordered/unreceived expectations
     */
    public long getAssertTime() {
        return this.assertTime;
    }

    /**
     * @return The endpoint overrides that modify the receiving endpoint
     */
    public Collection<EndpointOverride> getEndpointOverrides() {
        return Collections.unmodifiableCollection(this.endpointOverrides);
    }

    /**
     * @return The number of times the message will be sent to the endpoint
     */
    public int getSendCount() {
        return this.sendCount;
    }

    /**
     * @return The interval in milliseconds between sending multiple messages
     */
    public long getSendInterval() {
        return this.sendInterval;
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

    //Builder/DSL/Fluent API inheritance has been inspired by the blog: https://weblogs.java.net/node/642849
    public static class OrchestratedTestSpecificationBuilder<Builder extends MorcBuilder<Builder>> extends MorcBuilder<Builder> {

        private String description;
        private Map<String,MockDefinition> mockExpectations = new HashMap<>();
        private Queue<String> endpointOrdering = new LinkedList<>();
        private long assertTime = 15000l;
        private Collection<EndpointOverride> endpointOverrides = new ArrayList<>();
        private int sendCount = 1;
        private long sendInterval = 1000l;
        private int partCount = 1;
        private OrchestratedTestSpecification nextPart = null;
        //todo: add time to wait for all requests to be sent

        //final list of single processors and predicates
        private List<Processor> processors;
        private List<Predicate> predicates;

        private OrchestratedTestSpecificationBuilder nextPartBuilder;

        public OrchestratedTestSpecificationBuilder(String description, String endpointUri) {
            super (endpointUri);
            this.description = description;

            //we don't want to use POJO to receive messages
            endpointOverrides.add(new CxfEndpointOverride());
            endpointOverrides.add(new UrlConnectionOverride());
        }


        @SuppressWarnings("unchecked")
        public OrchestratedTestSpecification build() {
            if (nextPartBuilder != null) {
                nextPart = nextPartBuilder.build();
                partCount = nextPart.getPartCount() + 1;
            }

            processors = getProcessors();
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
        @SuppressWarnings("unchecked")
        public Builder addExpectation(MockDefinition.MockDefinitionBuilder mockDefinitionBuilder) {

            //we need to merge the expectations
            MockDefinition endpointExpectation = mockExpectations.get(mockDefinitionBuilder.getEndpointUri());

            int currentEndpointExpectationMessageCount = 0;

            if (endpointExpectation != null)
                currentEndpointExpectationMessageCount = endpointExpectation.getExpectedMessageCount();

            MockDefinition mergedExpectation =
                    mockDefinitionBuilder.build(endpointExpectation);

            int mergedEndpointExpectationMessageCount =
                    mergedExpectation.getExpectedMessageCount() - currentEndpointExpectationMessageCount;

            for (int i = 0; i < mergedEndpointExpectationMessageCount; i++)
                endpointOrdering.add(mergedExpectation.getEndpointUri());

            mockExpectations.put(mockDefinitionBuilder.getEndpointUri(), mergedExpectation);

            return self();
        }

        /**
         * A convenience method for adding multiple expectations at the same time
         */
        public Builder addExpectations(MockDefinition.MockDefinitionBuilder... expectationBuilders) {
            for (MockDefinition.MockDefinitionBuilder expectationBuilder : expectationBuilders) {
                addExpectation(expectationBuilder);
            }

            return self();
        }

        /**
         * @param override An override used for modifying an endpoint for *receiving* a message
         */
        public Builder addEndpointOverride(EndpointOverride override) {
            endpointOverrides.add(override);
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
         * @param sendCount The number of times to send the message, defaults to 1
         */
        public Builder sendCount(int sendCount) {
            if (sendCount < 1)
                throw new IllegalArgumentException("You must be able to send at least one message with sendCount");
            this.sendCount = sendCount;
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
            return (Builder) addEndpoint(endpointUri, this.getClass());
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

    }

    @SuppressWarnings("unchecked")
    private OrchestratedTestSpecification(OrchestratedTestSpecificationBuilder builder) {
        this.description = builder.description;
        this.endpointUri = builder.getEndpointUri();
        this.mockDefinitions = builder.mockExpectations.entrySet();
        this.assertTime = builder.assertTime;
        this.endpointOverrides = builder.endpointOverrides;
        this.sendCount = builder.sendCount;
        this.sendInterval = builder.sendInterval;
        this.partCount = builder.partCount;
        this.nextPart = builder.nextPart;
        this.endpointOrdering = builder.endpointOrdering;
        this.processors = builder.processors;
        this.predicates = builder.predicates;
    }
}

