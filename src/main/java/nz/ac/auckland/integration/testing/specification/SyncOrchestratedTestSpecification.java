package nz.ac.auckland.integration.testing.specification;

import nz.ac.auckland.integration.testing.resource.HeadersTestResource;
import nz.ac.auckland.integration.testing.resource.TestResource;
import nz.ac.auckland.integration.testing.validators.ExceptionValidator;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Endpoint;
import org.apache.camel.ProducerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * A synchronous orchestrated test makes a call to a target endpoint which provides a response. During
 * the request process the target may make a number of call outs to expectations which need to be satisfied.
 * The response body from the target will also be validated against the expected response body.
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class SyncOrchestratedTestSpecification extends OrchestratedTestSpecification {
    private Logger logger = LoggerFactory.getLogger(SyncOrchestratedTestSpecification.class);
    private TestResource<String> inputRequestBody;
    private HeadersTestResource inputRequestHeaders;
    private TestResource<String> expectedResponseBody;
    private boolean expectsExceptionResponse;
    private ExceptionValidator exceptionValidator;

    /**
     * @return The input request body that will be sent to the target endpoint
     */
    public TestResource<String> getInputRequestBody() {
        return inputRequestBody;
    }

    /**
     * @return The input request headers that will be sent to the target endpoint
     */
    public HeadersTestResource getInputRequestHeaders() {
        return inputRequestHeaders;
    }

    /**
     * @return The input request body that should be returned in order for the test to pass
     */
    public TestResource<String> getExpectedResponseBody() {
        return expectedResponseBody;
    }

    /**
     * @param template An Apache Camel template that can be used to send messages to a target endpoint
     * @return true if the message is successfully sent the the response body is as expected
     */
    public boolean sendInput(ProducerTemplate template) {
        try {
            String response;

            Endpoint endpoint = template.getCamelContext().getEndpoint(getTargetServiceUri());
            overrideEndpoint(endpoint);

            if (inputRequestBody != null && inputRequestHeaders != null)
                response = template.requestBodyAndHeaders(endpoint, inputRequestBody.getValue(), inputRequestHeaders.getValue(), String.class);
            else if (inputRequestHeaders != null)
                response = template.requestBodyAndHeaders(endpoint, "", inputRequestHeaders.getValue(), String.class);
            else if (inputRequestBody != null)
                response = template.requestBody(endpoint, inputRequestBody.getValue(), String.class);
            else response = template.requestBody(endpoint, "", String.class);

            return expectedResponseBody == null || expectedResponseBody.validate(response);

        } catch (CamelExecutionException e) {
            logger.info("An execution exception was encountered", e);
            if (expectsExceptionResponse && exceptionValidator == null) return true;
            if (expectsExceptionResponse) {
                return exceptionValidator.validate(e, expectedResponseBody);
            }

            throw e;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class Builder extends OrchestratedTestSpecification.AbstractBuilder<SyncOrchestratedTestSpecification, Builder> {

        private TestResource<String> inputRequestBody;
        private HeadersTestResource inputRequestHeaders;
        private TestResource<String> expectedResponseBody;
        private boolean expectsExceptionResponse = false;
        private ExceptionValidator exceptionValidator;

        public Builder(String endpointUri, String description) {
            super(endpointUri, description);
        }

        protected Builder self() {
            return this;
        }

        /**
         * @param inputRequestBody The input request body to send to the target service
         */
        public Builder requestBody(TestResource<String> inputRequestBody) {
            this.inputRequestBody = inputRequestBody;
            return self();
        }

        /**
         * @param inputRequestHeaders The input request headers to send to the target service
         */
        public Builder requestHeaders(HeadersTestResource inputRequestHeaders) {
            this.inputRequestHeaders = inputRequestHeaders;
            return self();
        }

        /**
         * @param expectedResponseBody The body that is expected to received back as a response
         */
        public Builder expectedResponseBody(TestResource<String> expectedResponseBody) {
            this.expectedResponseBody = expectedResponseBody;
            return self();
        }

        /**
         * An exception/fault is expected to be thrown in the response
         */
        public Builder expectsExceptionResponse() {
            this.expectsExceptionResponse = true;
            return self();
        }

        /**
         * @param exceptionValidator Something capable of validating this type of exception
         */
        public Builder exceptionValidator(ExceptionValidator exceptionValidator) {
            this.exceptionValidator = exceptionValidator;
            return self();
        }

        public SyncOrchestratedTestSpecification build() {
            return new SyncOrchestratedTestSpecification(this);
        }
    }

    protected SyncOrchestratedTestSpecification(Builder builder) {
        super(builder);

        this.inputRequestBody = builder.inputRequestBody;
        this.inputRequestHeaders = builder.inputRequestHeaders;
        this.expectedResponseBody = builder.expectedResponseBody;
        this.expectsExceptionResponse = builder.expectsExceptionResponse;
        this.exceptionValidator = builder.exceptionValidator;
    }

}
