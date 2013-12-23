package nz.ac.auckland.integration.testing.specification;

import nz.ac.auckland.integration.testing.resource.HeadersTestResource;
import nz.ac.auckland.integration.testing.resource.TestResource;
import nz.ac.auckland.integration.testing.validator.HeadersValidator;
import nz.ac.auckland.integration.testing.validator.Validator;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.util.ExchangeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * A synchronous orchestrated test makes a call to a target endpoint which provides a response. During
 * the request process the target may make a number of call outs to expectations which need to be satisfied.
 * The response body from the target will also be validated against the expected response body.
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class SyncOrchestratedTestSpecification extends OrchestratedTestSpecification {
    private static final Logger logger = LoggerFactory.getLogger(SyncOrchestratedTestSpecification.class);

    private Queue<TestResource> inputRequestBodies;
    private Queue<TestResource<Map<String, Object>>> inputRequestHeaders;
    private Queue<Validator> responseBodyValidators;
    private Queue<HeadersValidator> responseHeadersValidators;
    private boolean expectsExceptionResponse = false;

    /**
     * @param template An Apache Camel template that can be used to send messages to a target endpoint
     * @return true if the message is successfully sent the the response body is as expected
     */
    protected boolean sendInputInternal(ProducerTemplate template) {
        try {

            final TestResource inputBody;
            final TestResource<Map<String, Object>> inputHeaders;
            final Validator responseBodyValidator;
            final HeadersValidator responseHeadersValidator;

            //ensure we get all required resources in lock-step
            synchronized (this) {
                inputBody = inputRequestBodies.poll();
                inputHeaders = inputRequestHeaders.poll();
                responseBodyValidator = responseBodyValidators.poll();
                responseHeadersValidator = responseHeadersValidators.poll();
            }

            final Endpoint endpoint = template.getCamelContext().getEndpoint(getEndpointUri());
            overrideEndpoint(endpoint);

            Exchange response;

            if (inputBody != null && inputHeaders != null)
                response = template.request(endpoint, new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        exchange.getIn().setBody(inputBody.getValue());
                        exchange.getIn().setHeaders(inputHeaders.getValue());
                        logger.trace("Sending to endpoint: {} headers: {}, body: {}", new String[]{endpoint.toString(),
                                HeadersTestResource.formatHeaders(inputHeaders.getValue()),
                                exchange.getIn().getBody(String.class)});
                    }
                });
            else if (inputHeaders != null)
                response = template.request(endpoint, new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        exchange.getIn().setBody("");
                        exchange.getIn().setHeaders(inputHeaders.getValue());
                        logger.trace("Sending to endpoint: {} headers: {}, body: {}", new String[]{endpoint.toString(),
                                HeadersTestResource.formatHeaders(inputHeaders.getValue()),
                                exchange.getIn().getBody(String.class)});
                    }
                });
            else if (inputBody != null)
                response = template.request(endpoint, new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        exchange.getIn().setBody(inputBody.getValue());
                        logger.trace("Sending to endpoint: {} body: {}", new String[]{endpoint.toString(),
                                exchange.getIn().getBody(String.class)});
                    }
                });
            else
                response = template.request(endpoint, new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        exchange.getIn().setBody("");
                        logger.trace("Sending to endpoint: {} body: {}", new String[]{endpoint.toString(),
                                exchange.getIn().getBody(String.class)});
                    }
                });

            //Put the out message into in for consistency during validation
            ExchangeHelper.prepareOutToIn(response);

            Exception e = response.getException();

            logger.trace("Synchronous response headers: {}, body: {}",
                    HeadersTestResource.formatHeaders(response.getIn().getHeaders()), response.getIn().getBody(String.class));

            boolean validResponse = ((responseBodyValidator == null || responseBodyValidator.validate(response))
                    && (responseHeadersValidator == null || responseHeadersValidator.validate(response)));

            if (!expectsExceptionResponse && e != null) {
                logger.warn("An unexpected exception was encountered", e);
                return false;
            }

            return validResponse;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class Builder extends OrchestratedTestSpecification.AbstractBuilder<SyncOrchestratedTestSpecification, Builder> {

        private Queue<TestResource> inputRequestBodies = new LinkedList<>();
        private Queue<TestResource<Map<String, Object>>> inputRequestHeaders = new LinkedList<>();
        private Queue<Validator> responseBodyValidators = new LinkedList<>();
        private Queue<HeadersValidator> responseHeadersValidators = new LinkedList<>();
        private boolean expectsExceptionResponse;

        public Builder(String description, String endpointUri) {
            super(description, endpointUri);
        }

        protected Builder self() {
            return this;
        }

        public Builder requestBody(TestResource... resources) {
            Collections.addAll(inputRequestBodies, resources);
            return self();
        }

        public Builder requestBody(Enumeration<TestResource> resources) {
            while (resources.hasMoreElements()) {
                inputRequestBodies.add(resources.nextElement());
            }
            return self();
        }

        @SafeVarargs
        public final Builder requestHeaders(TestResource<Map<String, Object>>... resources) {
            Collections.addAll(inputRequestHeaders, resources);
            return self();
        }

        public Builder requestHeaders(Enumeration<TestResource<Map<String, Object>>> resources) {
            while (resources.hasMoreElements()) {
                inputRequestHeaders.add(resources.nextElement());
            }
            return self();
        }

        public Builder expectedResponseBody(Validator... validators) {
            Collections.addAll(this.responseBodyValidators, validators);
            return self();
        }

        public Builder expectedResponseBody(Enumeration<Validator> validators) {
            while (validators.hasMoreElements()) {
                responseBodyValidators.add(validators.nextElement());
            }
            return self();
        }

        public Builder expectedResponseHeaders(HeadersValidator... responseHeadersValidators) {
            Collections.addAll(this.responseHeadersValidators, responseHeadersValidators);
            return self();
        }

        @SafeVarargs
        public final Builder expectedResponseHeaders(TestResource<Map<String, Object>>... resources) {
            for (TestResource<Map<String, Object>> resource : resources) {
                this.responseHeadersValidators.add(new HeadersValidator(resource));
            }
            return self();
        }

        public Builder expectedResponseHeaders(Enumeration<HeadersValidator> resources) {
            while (resources.hasMoreElements()) {
                this.responseHeadersValidators.add(resources.nextElement());
            }
            return self();
        }

        public Builder expectedResponse(Validator... validators) {
            return this.expectedResponseBody(validators);
        }

        public Builder expectedResponse(Enumeration<Validator> validators) {
            return this.expectedResponseBody(validators);
        }

        public Builder expectsExceptionResponse() {
            this.expectsExceptionResponse = true;
            return self();
        }

        protected SyncOrchestratedTestSpecification buildInternal() {
            SyncOrchestratedTestSpecification specification = new SyncOrchestratedTestSpecification(this);

            logger.info("The endpoint {} will be sending {} request message bodies, {} request message headers, " +
                    "{} expected response body validators, and {} expected response headers validators",
                    new Object[]{specification.getEndpointUri(), inputRequestBodies.size(), inputRequestHeaders.size(),
                            responseBodyValidators.size(), responseHeadersValidators.size()});

            return specification;
        }
    }

    protected SyncOrchestratedTestSpecification(Builder builder) {
        super(builder);

        this.inputRequestBodies = builder.inputRequestBodies;
        this.inputRequestHeaders = builder.inputRequestHeaders;
        this.responseBodyValidators = builder.responseBodyValidators;
        this.responseHeadersValidators = builder.responseHeadersValidators;
    }

}
