package nz.ac.auckland.integration.testing.specification;

import nz.ac.auckland.integration.testing.resource.*;
import nz.ac.auckland.integration.testing.validator.*;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.util.ExchangeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A synchronous orchestrated test makes a call to a target endpoint which provides a response. During
 * the request process the target may make a number of call outs to expectations which need to be satisfied.
 * The response body from the target will also be validated against the expected response body.
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class SyncOrchestratedTestSpecification extends OrchestratedTestSpecification {
    private static final Logger logger = LoggerFactory.getLogger(SyncOrchestratedTestSpecification.class);
    private TestResource inputRequestBody;
    private HeadersTestResource inputRequestHeaders;
    private Validator responseBodyValidator;
    private boolean expectsExceptionResponse;
    private Validator exceptionResponseValidator;
    private Validator responseHeadersValidator;

    /**
     * @return The input request body that will be sent to the target endpoint
     */
    public TestResource getInputRequestBody() {
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
    public Validator getResponseBodyValidator() {
        return responseBodyValidator;
    }

    /**
     * @return The exception validator
     */
    public Validator getExceptionResponseValidator() {
        return exceptionResponseValidator;
    }

    /**
     * @return The response headers validator
     */
    public Validator getResponseHeadersValidator() {
        return responseHeadersValidator;
    }

    /**
     * @param template An Apache Camel template that can be used to send messages to a target endpoint
     * @return true if the message is successfully sent the the response body is as expected
     */
    public boolean sendInput(ProducerTemplate template) {
        try {
            Endpoint endpoint = template.getCamelContext().getEndpoint(getTargetServiceUri());
            overrideEndpoint(endpoint);

            Exchange response;

            if (inputRequestBody != null && inputRequestHeaders != null)
                response = template.request(endpoint, new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        exchange.getIn().setBody(inputRequestBody.getValue());
                        exchange.getIn().setHeaders(inputRequestHeaders.getValue());
                    }
                });
            else if (inputRequestHeaders != null)
                response = template.request(endpoint,new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        exchange.getIn().setBody("");
                        exchange.getIn().setHeaders(inputRequestHeaders.getValue());
                    }
                });
            else if (inputRequestBody != null)
                response = template.request(endpoint,new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        exchange.getIn().setBody(inputRequestBody.getValue());
                    }
                });
            else
                response = template.request(endpoint, new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        exchange.getIn().setBody("");
                    }
                });

            //Put the out message into in for consistency during validation
            ExchangeHelper.prepareOutToIn(response);

            Exception e = response.getException();

            if (e == null && (expectsExceptionResponse || exceptionResponseValidator != null)) {
                logger.warn("An exception was expected to be received");
                return false;
            }

            if (e != null && (!expectsExceptionResponse && exceptionResponseValidator == null)) {
                logger.warn("An unexpected exception was encountered",e);
                return false;
            }

            if (e != null) {
                logger.warn("An execution exception was encountered", e);
                //validator response always wins
                if (exceptionResponseValidator != null)
                    return exceptionResponseValidator.validate(response);
                //this will always be true
                return expectsExceptionResponse;
            }

            return ((responseBodyValidator == null || responseBodyValidator.validate(response))
                    && (responseHeadersValidator == null || responseHeadersValidator.validate(response)));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class Builder extends OrchestratedTestSpecification.AbstractBuilder<SyncOrchestratedTestSpecification, Builder> {

        private TestResource inputRequestBody;
        private HeadersTestResource inputRequestHeaders;
        private Validator responseBodyValidator;
        private boolean expectsExceptionResponse = false;
        private Validator exceptionResponseValidator;
        private Validator responseHeadersValidator;

        public Builder(String endpointUri, String description) {
            super(endpointUri, description);
        }

        protected Builder self() {
            return this;
        }

        /**
         * @param inputRequestBody The input request body to send to the target service
         */
        public Builder requestBody(StaticTestResource inputRequestBody) {
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
         * @param responseBodyValidator The body that is expected to received back as a response
         */
        public Builder expectedResponseBody(Validator responseBodyValidator) {
            this.responseBodyValidator = responseBodyValidator;
            return self();
        }

        /**
         * @param responseHeadersValidator A validator for the headers in the response
         */
        public Builder expectedResponseHeaders(Validator responseHeadersValidator) {
            this.responseHeadersValidator = responseHeadersValidator;
            return self();
        }

        /**
         * @param resource An XML resource which will be used to seed a validator
         */
        public Builder expectedResponseBody(XmlTestResource resource) {
            this.responseBodyValidator = new XmlValidator(resource);
            return self();
        }

        /**
         * @param resource A JSON resource which will be used to seed a validator
         */
        public Builder expectedResponseBody(JsonTestResource resource) {
            this.responseBodyValidator = new JsonValidator(resource);
            return self();
        }

        /**
         * @param resource A plain text resource which will be used to seed a validator
         */
        public Builder expectedResponseBody(PlainTextTestResource resource) {
            this.responseBodyValidator = new PlainTextValidator(resource);
            return self();
        }

        /**
         * @param resource A header test resource which will be used to seed a validator
         */
        public Builder expectedResponseHeaders(HeadersTestResource resource) {
            this.responseHeadersValidator = new HeadersValidator(resource);
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
         * @param exceptionResponseValidator Something capable of validating the expected type of exception
         */
        public Builder exceptionResponseValidator(Validator exceptionResponseValidator) {
            this.exceptionResponseValidator = exceptionResponseValidator;
            return self();
        }

        public SyncOrchestratedTestSpecification build() {
            return new SyncOrchestratedTestSpecification(this);
        }
    }

    protected SyncOrchestratedTestSpecification(Builder builder) {
        super(builder);

        if ((builder.responseBodyValidator != null || builder.responseHeadersValidator != null)
                && (builder.expectsExceptionResponse || builder.exceptionResponseValidator != null))
            throw new IllegalArgumentException("You cannot set a response validator if an exception is expected");

        this.inputRequestBody = builder.inputRequestBody;
        this.inputRequestHeaders = builder.inputRequestHeaders;
        this.responseBodyValidator = builder.responseBodyValidator;
        this.expectsExceptionResponse = builder.expectsExceptionResponse;
        this.exceptionResponseValidator = builder.exceptionResponseValidator;
        this.responseHeadersValidator = builder.responseHeadersValidator;
    }

}
