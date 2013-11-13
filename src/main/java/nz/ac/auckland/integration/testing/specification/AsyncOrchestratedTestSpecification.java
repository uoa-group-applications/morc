package nz.ac.auckland.integration.testing.specification;

import nz.ac.auckland.integration.testing.resource.HeadersTestResource;
import nz.ac.auckland.integration.testing.resource.StaticTestResource;
import nz.ac.auckland.integration.testing.resource.TestResource;
import org.apache.camel.Endpoint;
import org.apache.camel.ProducerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A complete test specification including all expectations for asynchronously sending a message
 * to a target destination.
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class AsyncOrchestratedTestSpecification extends OrchestratedTestSpecification {
    private static final Logger logger = LoggerFactory.getLogger(AsyncOrchestratedTestSpecification.class);
    private HeadersTestResource inputMessageHeaders;
    private TestResource inputMessageBody;

    /**
     * @return The message headers that will be sent to the target endpoint
     */
    public StaticTestResource getInputMessageHeaders() {
        return inputMessageHeaders;
    }

    /**
     * @return The message body that will be sent to the target endpoint
     */
    public TestResource getInputMessageBody() {
        return inputMessageBody;
    }

    /**
     * @param template An Apache Camel template that can be used to send messages to a target endpoint
     * @return true as there is no response to validate
     */
    protected boolean sendInputInternal(ProducerTemplate template) {
        try {
            Endpoint endpoint = template.getCamelContext().getEndpoint(getTargetServiceUri());
            overrideEndpoint(endpoint);

            if (inputMessageBody != null && inputMessageHeaders != null) {
                logger.trace("Sending to endpoint: {} headers: {}, body: {}", new String[] {endpoint.toString(),
                    HeadersTestResource.formatHeaders(inputMessageHeaders.getValue()),
                    template.getCamelContext().getTypeConverter().convertTo(String.class,inputMessageBody.getValue())});
                template.sendBodyAndHeaders(endpoint, inputMessageBody.getValue(), inputMessageHeaders.getValue());
            }
            else if (inputMessageHeaders != null) {
                logger.trace("Sending to endpoint: {} headers: {}, body: {}", new String[] {endpoint.toString(),
                    HeadersTestResource.formatHeaders(inputMessageHeaders.getValue()),
                    template.getCamelContext().getTypeConverter().convertTo(String.class,"")});
                template.sendBodyAndHeaders(endpoint, "", inputMessageHeaders.getValue());
            } else if (inputMessageBody != null) {
                logger.trace("Sending to endpoint: {} body: {}", new String[] {endpoint.toString(),
                    template.getCamelContext().getTypeConverter().convertTo(String.class,inputMessageBody.getValue())});
                template.sendBody(endpoint, inputMessageBody.getValue());
            } else {
                logger.trace("Sending to endpoint: {} body: {}", new String[] {endpoint.toString(),""});
                template.sendBody(endpoint, "");
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    public static class Builder extends AsyncOrchestratedTestSpecification.AbstractBuilder<AsyncOrchestratedTestSpecification, Builder> {

        private HeadersTestResource inputMessageHeaders;
        private TestResource inputMessageBody;

        public Builder(String endpointUri, String description) {
            super(endpointUri, description);
        }

        protected Builder self() {
            return this;
        }

        /**
         * @param inputMessageBody The message body to send to the target endpoint
         */
        public Builder inputMessage(TestResource inputMessageBody) {
            this.inputMessageBody = inputMessageBody;
            return self();
        }

        /**
         * @param inputMessageHeaders The message headers to send to the target endpoint
         */
        public Builder inputHeaders(HeadersTestResource inputMessageHeaders) {
            this.inputMessageHeaders = inputMessageHeaders;
            return self();
        }

        /**
         * @throws IllegalArgumentException if no expectations are specified
         */
        public AsyncOrchestratedTestSpecification build() {
            AsyncOrchestratedTestSpecification specification = new AsyncOrchestratedTestSpecification(this);
            if (specification.getMockExpectations().size() == 0)
                throw new IllegalArgumentException("At least 1 mock expectation must be set for an AsyncOrchestratedTestSpecification");
            return specification;
        }
    }

    protected AsyncOrchestratedTestSpecification(Builder builder) {
        super(builder);

        this.inputMessageHeaders = builder.inputMessageHeaders;
        this.inputMessageBody = builder.inputMessageBody;
    }
}
