package nz.ac.auckland.integration.testing.specification;

import nz.ac.auckland.integration.testing.endpointoverride.CxfEndpointOverride;
import nz.ac.auckland.integration.testing.endpointoverride.EndpointOverride;
import nz.ac.auckland.integration.testing.endpointoverride.UrlConnectionOverride;
import nz.ac.auckland.integration.testing.mock.MockDefinition;
import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
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
public abstract class OrchestratedTestSpecification {
    private static final Logger logger = LoggerFactory.getLogger(OrchestratedTestSpecification.class);
    private String description;
    private String endpointUri;
    private final Set<MockDefinition> mockDefinitions;
    private long sleepForTestCompletion;
    private Collection<EndpointOverride> endpointOverrides = new ArrayList<>();
    private List<MockDefinition> endpointOrdering;
    private int sendCount;
    private long sendInterval;
    private int totalExpectedMessageCount;
    private int partCount;
    private OrchestratedTestSpecification nextPart;

    /**
     * @return A description that explains what this tests is doing
     */
    public String getDescription() {
        return description;
    }

    public List<MockDefinition> getEndpointOrdering() {
        return Collections.unmodifiableList(this.endpointOrdering);
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
    public long getSleepForTestCompletion() {
        return this.sleepForTestCompletion;
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

    protected void overrideEndpoint(Endpoint endpoint) {
        for (EndpointOverride override : endpointOverrides) {
            override.overrideEndpoint(endpoint);
        }
    }

    public String getEndpointUri() {
        return endpointUri;
    }

    public void setUp(final CamelContext context) throws Exception {
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                for (MockDefinition expectation : mockDefinitions) {
                    configureRoute(expectation.getExpectationFeederRoute());
                }
            }
        });

    }

    public void tearDown(CamelContext context) {

    }

    /**
     * @param template An Apache Camel template that can be used to send messages to a target endpoint
     * @return Returns true if the message was successfully sent and, if there's a response, that it is valid
     */
    public void sendInput(ProducerTemplate template) {

        int i = 0;
        //will replace this with a runner/closure when JDK8 is released
        do {
            try {
                if (i != 0) Thread.sleep(sendInterval);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            logger.debug("Sending input message {}", i + 1);
            boolean response = sendInputInternal(template);
            if (!response) {
                logger.warn("Failed on sending input message {}", i + 1);
                return;
            }

            i++;
        } while (i < sendCount);

    }

    public void assertIsSatisfied() {
        //countdown latch

        //check endpoint ordering

        //for each expectation we ensure it is running correctly
    }

    protected abstract boolean sendInputInternal(ProducerTemplate template);

    //Builder/DSL/Fluent API inheritance has been inspired by the blog: https://weblogs.java.net/node/642849
    public static abstract class AbstractBuilder<Product extends OrchestratedTestSpecification,
            Builder extends AbstractBuilder<Product, Builder>> {

        private String description;
        private String endpointUri;
        private Map<String,MockDefinition> mockExpectations;
        private Queue<String> endpointOrdering;
        private long sleepForTestCompletion = 15000;
        private Collection<EndpointOverride> endpointOverrides;
        private int sendCount = 1;
        private long sendInterval = 1000l;
        private int partCount = 1;
        private OrchestratedTestSpecification nextPart = null;
        private int totalExpectedMessageCount;

        private AbstractBuilder nextPartBuilder;

        private Map<String, MockDefinition> mockExpectationByEndpoint = new HashMap<>();

        public AbstractBuilder(String description, String endpointUri) {
            this.description = description;
            mockExpectations = new HashMap<>();
            endpointOverrides = new ArrayList<>();
            //we don't want to use POJO to receive messages
            endpointOverrides.add(new CxfEndpointOverride());
            endpointOverrides.add(new UrlConnectionOverride());
            try {
                this.endpointUri = URISupport.normalizeUri(endpointUri);
            } catch (URISyntaxException | UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }

        protected abstract Builder self();

        //todo: can we get rid of this?
        protected abstract Product buildInternal();

        @SuppressWarnings("unchecked")
        public Product build() {
            //todo add endpoint overrides
            //todo add interceptor

            if (nextPartBuilder != null) {
                nextPart = nextPartBuilder.build();
                partCount = nextPart.getPartCount() + 1;
            }

            for (MockDefinition expectation : mockExpectations.values()) {
                totalExpectedMessageCount += expectation.getExpectedMessageCount();
            }
            return this.buildInternal();
        }

        /**
         * We use an EndpointSubscriber Builder here as we have to do additional configuration before creation. This includes
         * setting up the expected index that an expectation is received which means that previous calls to
         * MockDefinition.receivedAt() will be ignored.
         *
         * @param expectationBuilder The expectation builder used to seed the expectation
         * @throws IllegalArgumentException if expectations to the same endpoint have different ordering requirements
         */
        @SuppressWarnings("unchecked")
        public Builder addExpectation(MockDefinition.MockDefinitionBuilder expectationBuilder) {

            //we need to merge the expectations
            MockDefinition endpointExpectation = mockExpectations.get(expectationBuilder.getEndpointUri());

            int currentEndpointExpectationMessageCount = 0;

            if (endpointExpectation != null)
                currentEndpointExpectationMessageCount = endpointExpectation.getExpectedMessageCount();

            MockDefinition mergedExpectation =
                    expectationBuilder.build(endpointExpectation);

            int mergedEndpointExpectationMessageCount =
                    mergedExpectation.getExpectedMessageCount() - currentEndpointExpectationMessageCount;

            for (int i = 0; i < mergedEndpointExpectationMessageCount; i++)
                endpointOrdering.add(mergedExpectation.getEndpointUri());

            mockExpectations.put(expectationBuilder.getEndpointUri(),mergedExpectation);

            return self();
        }

        /**
         * A convenience method for adding multiple expectations at the same time
         */
        public Builder addExpectations(MockDefinition.MockDefinitionBuilder... expectationBuilders) {
            for (MockDefinition.MockDefinitionBuilder expectationBuilder : expectationBuilders) {
                self().addExpectation(expectationBuilder);
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
            this.sleepForTestCompletion = sleepForTestCompletion;
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
        public <T extends AbstractBuilder<?, ?>> T addEndpoint(String endpointUri, Class<T> clazz) {
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
    private OrchestratedTestSpecification(AbstractBuilder builder) {
        this.description = builder.description;
        this.endpointUri = builder.endpointUri;
        this.mockDefinitions = builder.mockExpectations.entrySet();
        this.sleepForTestCompletion = builder.sleepForTestCompletion;
        this.endpointOverrides = builder.endpointOverrides;
        this.sendCount = builder.sendCount;
        this.sendInterval = builder.sendInterval;
        this.partCount = builder.partCount;
        this.nextPart = builder.nextPart;
        this.totalExpectedMessageCount = builder.totalExpectedMessageCount;
    }
}

