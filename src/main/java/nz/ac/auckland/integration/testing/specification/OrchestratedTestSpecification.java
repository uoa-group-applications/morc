package nz.ac.auckland.integration.testing.specification;

import nz.ac.auckland.integration.testing.endpointoverride.CxfEndpointOverride;
import nz.ac.auckland.integration.testing.endpointoverride.EndpointOverride;
import nz.ac.auckland.integration.testing.expectation.MockExpectation;
import org.apache.camel.Endpoint;
import org.apache.camel.ProducerTemplate;

import java.util.*;

/**
 * An orchestrated test is a declarative way of specifying a target for a message with a number
 * of expectations related to call-outs required for the process that the original message triggered
 * is deemed to be correct.
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public abstract class OrchestratedTestSpecification {
    private String description;
    private String targetServiceUri;
    private List<MockExpectation> mockExpectations;
    private long sleepForTestCompletion;
    private Collection<EndpointOverride> endpointOverrides = new ArrayList<>();


    /**
     * @return A description that explains what this tests is doing
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return An Apache Camel URI that a message/request will be sent to
     */
    public String getTargetServiceUri() {
        return targetServiceUri;
    }

    /**
     * @return A list of expectations that need to be satisfied for the test to pass
     */
    public List<MockExpectation> getMockExpectations() {
        return Collections.unmodifiableList(mockExpectations);
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
        return this.endpointOverrides;
    }

    protected void overrideEndpoint(Endpoint endpoint) {
        for (EndpointOverride override : endpointOverrides) {
            override.overrideEndpoint(endpoint);
        }
    }

    /**
     * @param template An Apache Camel template that can be used to send messages to a target endpoint
     * @return Returns true if the message was successfully sent and, if there's a response, that it is valid
     */
    public abstract boolean sendInput(ProducerTemplate template);

    //Builder/DSL/Fluent API inheritance has been inspired by the blog: https://weblogs.java.net/node/642849
    public static abstract class AbstractBuilder<Product extends OrchestratedTestSpecification, Builder extends AbstractBuilder<Product, Builder>> {

        private String description;
        private String targetServiceUri;
        private List<MockExpectation> mockExpectations;
        private int currentExpectationReceivedAtIndex = 0;
        private long sleepForTestCompletion = 15000;
        private Collection<EndpointOverride> endpointOverrides;

        private Map<String, MockExpectation> mockExpectationByEndpoint = new HashMap<>();

        protected abstract Builder self();

        public abstract Product build();

        public AbstractBuilder(String targetServiceUri, String description) {
            this.targetServiceUri = targetServiceUri;
            this.description = description;
            mockExpectations = new ArrayList<>();
            endpointOverrides = new ArrayList<>();
            //we don't want to use POJO to receive messages
            endpointOverrides.add(new CxfEndpointOverride());
        }

        /**
         * We use an Expectation Builder here as we have to do additional configuration before creation. This includes
         * setting up the expected index that an expectation is received which means that previous calls to
         * MockExpectation.receivedAt() will be ignored.
         *
         * @param expectationBuilder The expectation builder used to seed the expectation
         * @throws IllegalArgumentException if expectations to the same endpoint have different ordering requirements
         */
        public Builder addExpectation(MockExpectation.AbstractBuilder expectationBuilder) {

            expectationBuilder.receivedAt(currentExpectationReceivedAtIndex);

            MockExpectation expectation = expectationBuilder.build();

            if (!mockExpectationByEndpoint.containsKey(expectation.getEndpointUri()))
                mockExpectationByEndpoint.put(expectation.getEndpointUri(), expectation);

            if (expectation.isEndpointOrdered() != mockExpectationByEndpoint.get(expectation.getEndpointUri()).isEndpointOrdered())
                throw new IllegalArgumentException("The endpoint for URI " + expectation.getEndpointUri() + " must have the same ordering " +
                        "requirements for each expectation");

            currentExpectationReceivedAtIndex += expectation.getExpectedMessageCount();

            mockExpectations.add(expectation);

            return self();
        }

        /**
         * @param override An override used for modifying an endpoint for *receiving* a message
         * @return
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

    }

    protected OrchestratedTestSpecification(AbstractBuilder builder) {
        this.description = builder.description;
        this.targetServiceUri = builder.targetServiceUri;
        this.mockExpectations = builder.mockExpectations;
        this.sleepForTestCompletion = builder.sleepForTestCompletion;
        this.endpointOverrides = builder.endpointOverrides;
    }

}
