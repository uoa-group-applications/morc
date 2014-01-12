package nz.ac.auckland.integration.testing.specification;

import nz.ac.auckland.integration.testing.resource.HeadersTestResource;
import nz.ac.auckland.integration.testing.resource.TestResource;
import nz.ac.auckland.integration.testing.predicate.HeadersPredicate;
import org.apache.camel.*;
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
    private Queue<Predicate> responseBodyPredicates;
    private Queue<HeadersPredicate> responseHeadersPredicates;
    private boolean expectsExceptionResponse = false;

    @Override
    public void assertIsSatisfied() {
        super.assertIsSatisfied();
    }

    /**
     * @param template An Apache Camel template that can be used to send messages to a target endpoint
     * @return true if the message is successfully sent the the response body is as expected
     */
    protected boolean sendInputInternal(ProducerTemplate template) {
        try {
            final TestResource inputBody;
            final TestResource<Map<String, Object>> inputHeaders;
            final Predicate responseBodyPredicate;
            final HeadersPredicate responseHeadersPredicate;

            //ensure we get all required resources in lock-step
            synchronized (this) {
                inputBody = inputRequestBodies.poll();
                inputHeaders = inputRequestHeaders.poll();
                responseBodyPredicate = responseBodyPredicates.poll();
                responseHeadersPredicate = responseHeadersPredicates.poll();
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

            boolean validResponse = ((responseBodyPredicate == null || responseBodyPredicate.matches(response))
                    && (responseHeadersPredicate == null || responseHeadersPredicate.matches(response)));

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
        private Queue<Predicate> responseBodyPredicates = new LinkedList<>();
        private Queue<HeadersPredicate> responseHeadersPredicates = new LinkedList<>();
        private boolean expectsExceptionResponse;

        public Builder(String description, String endpointUri) {
            super(description, endpointUri);
        }

        protected Builder self() {
            return this;
        }

        /**
         * @param resources A collection of test resources that can be used to send request bodies to a target endpoint
         */
        public Builder requestBody(TestResource... resources) {
            Collections.addAll(inputRequestBodies, resources);
            return self();
        }

        /**
         * @param resources An enumeration that will be iterated over for sending request bodies to a target endpoint
         */
        public Builder requestBody(Enumeration<TestResource> resources) {
            while (resources.hasMoreElements()) {
                inputRequestBodies.add(resources.nextElement());
            }
            return self();
        }

        /**
         * @param resources A collection of test header resources that can be used to send request headers to a target endpoint
         */
        @SafeVarargs
        public final Builder requestHeaders(TestResource<Map<String, Object>>... resources) {
            Collections.addAll(inputRequestHeaders, resources);
            return self();
        }

        /**
         * @param resources An enumeration that will be iterated over for sending request bodies to a target endpoint
         */
        public Builder requestHeaders(Enumeration<TestResource<Map<String, Object>>> resources) {
            while (resources.hasMoreElements()) {
                inputRequestHeaders.add(resources.nextElement());
            }
            return self();
        }

        /**
         * @param validators A collection of validators for the response body back from a callout
         */
        public Builder expectedResponseBody(Predicate... predicates) {
            Collections.addAll(this.responseBodyPredicates, predicates);
            return self();
        }

        /**
         * @param validators An enumeration that will be iterated over for callout body validation
         */
        public Builder expectedResponseBody(Enumeration<Predicate> predicates) {
            while (predicates.hasMoreElements()) {
                responseBodyPredicates.add(predicates.nextElement());
            }
            return self();
        }

        /**
         * @param responseHeadersPredicates A collection of validators for response headers back from a callout
         */
        public Builder expectedResponseHeaders(HeadersPredicate... responseHeadersPredicates) {
            Collections.addAll(this.responseHeadersPredicates, responseHeadersPredicates);
            return self();
        }

        /**
         * @param resources A collection of validators for response headers back from a callout
         */
        @SafeVarargs
        public final Builder expectedResponseHeaders(TestResource<Map<String, Object>>... resources) {
            for (TestResource<Map<String, Object>> resource : resources) {
                this.responseHeadersPredicates.add(new HeadersPredicate(resource));
            }
            return self();
        }

        /**
         * @param resources An enumeration that will be iterated over for header validation from a callout
         */
        public Builder expectedResponseHeaders(Enumeration<HeadersPredicate> resources) {
            while (resources.hasMoreElements()) {
                this.responseHeadersPredicates.add(resources.nextElement());
            }
            return self();
        }

        /**
         * @param validators A collection of validators for validating exchange responses for a callout
         */
        public Builder expectedResponse(Predicate... predicators) {
            return this.expectedResponseBody(predicators);
        }

        /**
         * @param validators An enumeration that will be iterated over for header validation for a callout
         */
        public Builder expectedResponse(Enumeration<Predicate> predicators) {
            return this.expectedResponseBody(predicators);
        }

        /**
         * Whether an exception should be expected back for this request
         */
        public Builder expectsExceptionResponse() {
            this.expectsExceptionResponse = true;
            return self();
        }

        protected SyncOrchestratedTestSpecification buildInternal() {
            SyncOrchestratedTestSpecification specification = new SyncOrchestratedTestSpecification(this);

            logger.info("The endpoint {} will be sending {} request message bodies, {} request message headers, " +
                    "{} expected response body predicates, and {} expected response headers predicate",
                    new Object[]{specification.getEndpointUri(), inputRequestBodies.size(), inputRequestHeaders.size(),
                            responseBodyPredicates.size(), responseHeadersPredicates.size()});

            return specification;
        }
    }

    protected SyncOrchestratedTestSpecification(Builder builder) {
        super(builder);

        this.inputRequestBodies = builder.inputRequestBodies;
        this.inputRequestHeaders = builder.inputRequestHeaders;
        this.responseBodyPredicates = builder.responseBodyPredicates;
        this.responseHeadersPredicates = builder.responseHeadersPredicates;
        this.expectsExceptionResponse = builder.expectsExceptionResponse;
    }

}
