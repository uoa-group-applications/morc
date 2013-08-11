package nz.ac.auckland.integration.testing.specification;

import nz.ac.auckland.integration.testing.resource.HeadersTestResource;
import nz.ac.auckland.integration.testing.resource.TestResource;
import org.apache.camel.ProducerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * A complete test specification including all expectations for asynchronously sending a message
 * to a target destination.
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class AsyncOrchestratedTestSpecification extends OrchestratedTestSpecification {
    private Logger logger = LoggerFactory.getLogger(AsyncOrchestratedTestSpecification.class);
    private HeadersTestResource inputMessageHeaders;
    private TestResource inputMessageBody;

    /**
     * @return The message headers that will be sent to the target endpoint
     */
    public TestResource getInputMessageHeaders() {
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
    public boolean sendInput(ProducerTemplate template) {
        try {
            if (inputMessageBody != null && inputMessageHeaders != null)
                template.sendBodyAndHeaders(getTargetServiceUri(), inputMessageBody.getValue(), inputMessageHeaders.getValue());
            else if (inputMessageHeaders != null)
                template.sendBodyAndHeaders(getTargetServiceUri(), "", inputMessageHeaders.getValue());
            else if (inputMessageBody != null)
                template.sendBody(getTargetServiceUri(), inputMessageBody.getValue());
            else
                template.sendBody(getTargetServiceUri(), "");

        } catch (IOException e) {
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
        public Builder inputMessage(TestResource<String> inputMessageBody) {
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
