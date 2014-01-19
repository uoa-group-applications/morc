package nz.ac.auckland.integration.testing.specification;

import nz.ac.auckland.integration.testing.resource.HeadersTestResource;
import nz.ac.auckland.integration.testing.resource.TestResource;
import org.apache.camel.Endpoint;
import org.apache.camel.ProducerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * A complete test specification including all expectations for asynchronously sending a message
 * to a target destination.
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class AsyncOrchestratedTestSpecification extends OrchestratedTestSpecification {
    private static final Logger logger = LoggerFactory.getLogger(AsyncOrchestratedTestSpecification.class);
    private Queue<TestResource<Map<String, Object>>> inputMessageHeaders;
    private Queue<TestResource> inputMessageBodies;

    /**
     * @param template An Apache Camel template that can be used to send messages to a target endpoint
     * @return true as there is no response to validate
     */
    protected boolean sendInputInternal(ProducerTemplate template) {

        TestResource<Map<String, Object>> inputHeaders;
        TestResource inputMessageBody;

        //ensure we have the request bodies and header in lock-step
        synchronized (this) {
            inputHeaders = inputMessageHeaders.poll();
            inputMessageBody = inputMessageBodies.poll();
        }

        try {
            Endpoint endpoint = template.getCamelContext().getEndpoint(getEndpointUri());
            overrideEndpoint(endpoint);

            if (inputMessageBody != null && inputHeaders != null) {
                logger.trace("Sending to endpoint: {} headers: {}, body: {}", new String[]{endpoint.toString(),
                        HeadersTestResource.formatHeaders(inputHeaders.getValue()),
                        template.getCamelContext().getTypeConverter().convertTo(String.class, inputMessageBody.getValue())});
                template.sendBodyAndHeaders(endpoint, inputMessageBody.getValue(), inputHeaders.getValue());
            } else if (inputHeaders != null) {
                logger.trace("Sending to endpoint: {} headers: {}, body: {}", new String[]{endpoint.toString(),
                        HeadersTestResource.formatHeaders(inputHeaders.getValue()),
                        template.getCamelContext().getTypeConverter().convertTo(String.class, "")});
                template.sendBodyAndHeaders(endpoint, "", inputHeaders.getValue());
            } else if (inputMessageBody != null) {
                logger.trace("Sending to endpoint: {} body: {}", new String[]{endpoint.toString(),
                        template.getCamelContext().getTypeConverter().convertTo(String.class, inputMessageBody.getValue())});
                template.sendBody(endpoint, inputMessageBody.getValue());
            } else {
                logger.trace("Sending to endpoint: {} body: {}", new String[]{endpoint.toString(), ""});
                template.sendBody(endpoint, "");
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    public static class Builder extends AsyncOrchestratedTestSpecification.AbstractBuilder<AsyncOrchestratedTestSpecification, Builder> {

        private Queue<TestResource<Map<String, Object>>> inputMessageHeaders = new LinkedList<>();
        private Queue<TestResource> inputMessageBodies = new LinkedList<>();

        public Builder(String description, String endpointUri) {
            super(description, endpointUri);
        }

        protected Builder self() {
            return this;
        }

        public Builder inputMessage(TestResource... resources) {
            Collections.addAll(this.inputMessageBodies, resources);
            return self();
        }

        public Builder inputMessage(Enumeration<TestResource> resources) {
            while (resources.hasMoreElements()) {
                this.inputMessageBodies.add(resources.nextElement());
            }
            return self();
        }

        @SafeVarargs
        public final Builder inputHeaders(TestResource<Map<String, Object>>... resources) {
            Collections.addAll(this.inputMessageHeaders, resources);
            return self();
        }

        public Builder inputHeaders(Enumeration<TestResource<Map<String, Object>>> resources) {
            while (resources.hasMoreElements()) {
                this.inputMessageHeaders.add(resources.nextElement());
            }
            return self();
        }

        /**
         * @throws IllegalArgumentException if no expectations are specified
         */
        protected AsyncOrchestratedTestSpecification buildInternal() {
            AsyncOrchestratedTestSpecification specification = new AsyncOrchestratedTestSpecification(this);
            if (specification.getMockDefinitions().size() == 0)
                throw new IllegalArgumentException("At least 1 mock expectation must be set for an AsyncOrchestratedTestSpecification");
            logger.info("The endpoint {} will be sending {} input message bodies and {} input message headers",
                    new Object[]{specification.getEndpointUri(), inputMessageBodies.size(), inputMessageHeaders.size()});
            return specification;
        }
    }

    protected AsyncOrchestratedTestSpecification(Builder builder) {
        super(builder);

        this.inputMessageHeaders = builder.inputMessageHeaders;
        this.inputMessageBodies = builder.inputMessageBodies;
    }
}
