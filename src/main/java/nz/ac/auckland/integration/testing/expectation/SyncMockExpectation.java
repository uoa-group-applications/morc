package nz.ac.auckland.integration.testing.expectation;

import nz.ac.auckland.integration.testing.answer.Answer;
import nz.ac.auckland.integration.testing.resource.TestResource;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * An expectation that provides a message response back to the message
 * consumer
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class SyncMockExpectation extends ContentMockExpectation {
    protected Answer responseBodyAnswer;
    protected Answer<Map<String, Object>> responseHeadersAnswer;
    private static final Logger logger = LoggerFactory.getLogger(SyncMockExpectation.class);

    /**
     * Sets the exchange out body to the provided response body and headers
     * Synchronized so that the body response and header response shift in lock-step
     *
     * @param exchange The Camel exchange that needs to be modified, or handled once it has been received
     * @throws Exception
     */
    public synchronized void handleReceivedExchange(Exchange exchange) throws Exception {
        if (responseBodyAnswer != null) exchange.getOut().setBody(responseBodyAnswer.response(exchange));
        else exchange.getOut().setBody("");
        if (responseHeadersAnswer != null) exchange.getOut().setHeaders(responseHeadersAnswer.response(exchange));
    }

    public String getType() {
        return "sync";
    }

    public static class Builder extends Init<SyncMockExpectation, Builder, Object> {
        public Builder(String endpointUri) {
            super(endpointUri);
        }

        protected SyncMockExpectation buildInternal() {
            return new SyncMockExpectation(this);
        }
    }

    protected abstract static class Init<Product, Builder extends Init<Product, Builder,T>, T>
            extends ContentMockExpectation.AbstractContentBuilder<SyncMockExpectation, Builder> {

        protected Answer<T> responseBodyAnswer;
        protected Answer<Map<String, Object>> responseHeadersAnswer;

        public Init(String endpointUri) {
            super(endpointUri);
        }

        /**
         * @param providedResponseBody The body that should be returned back to the client
         */
        public Builder responseBody(Answer<T> providedResponseBody) {
            this.responseBodyAnswer = providedResponseBody;
            return self();
        }

        @SuppressWarnings("unchecked")
        public final Builder responseBody(TestResource<T>... resources) {
            if (resources.length == 0)
                logger.warn("No test resource response bodies were provided for endpoint %s, this is not recommended",endpointUri);

            this.responseBodyAnswer = new AggregatedTestResourceAnswer<>(resources);
            return self();
        }

        @SuppressWarnings("unchecked")
        public Builder responseBody(Enumeration<TestResource<T>> resources) {
            if (!resources.hasMoreElements())
                logger.warn("The enumeration provided no response bodies for endpoint %s, this is not recommended",endpointUri);

            List<TestResource<T>> remainingResources = new ArrayList<>();

            while (resources.hasMoreElements()) {
                remainingResources.add(resources.nextElement());
            }

            return responseBody((TestResource<T>[])remainingResources.toArray());
        }

        /**
         * @param providedResponseHeaders The headers that should be returned back to the client
         */
        public Builder responseHeaders(Answer<Map<String, Object>> providedResponseHeaders) {
            this.responseHeadersAnswer = providedResponseHeaders;
            return self();
        }

        @SuppressWarnings("unchecked")
        public Builder responseHeaders(TestResource<Map<String,Object>>... resources) {
            if (resources.length == 0)
                logger.warn("No test resource response headers were provided for endpoint %s, this is not recommended",endpointUri);

            this.responseHeadersAnswer = new AggregatedTestResourceAnswer<>(resources);
            return self();
        }

        @SuppressWarnings("unchecked")
        public Builder responseHeaders(Enumeration<TestResource<Map<String,Object>>> resources) {
            if (!resources.hasMoreElements())
                logger.warn("The enumeration provided no response headers for endpoint %s, this is not recommended",endpointUri);

            List<TestResource<Map<String,Object>>> remainingResources = new ArrayList<>();

            while (resources.hasMoreElements()) {
                remainingResources.add(resources.nextElement());
            }

            return responseBody((TestResource<T>[])remainingResources.toArray());
        }

        @SuppressWarnings("unchecked")
        protected Builder self() {
            //this may throw an exception if the implementation isn't complete
            return (Builder) this;
        }

    }

    @SuppressWarnings("unchecked")
    protected SyncMockExpectation(Init builder) {
        super(builder);

        this.responseBodyAnswer = builder.responseBodyAnswer;
        this.responseHeadersAnswer = builder.responseHeadersAnswer;
    }

}

class AggregatedTestResourceAnswer<T> implements Answer {
    private Queue<T> values = new ConcurrentLinkedQueue<>();

    @SafeVarargs
    public AggregatedTestResourceAnswer(TestResource<T>... remaining) {
       try {
           for (TestResource<T> resource: remaining) {
               values.add(resource.getValue());
           }
       } catch (Exception e) {
           throw new RuntimeException(e);
       }
    }

    @Override
    public T response(Exchange exchange) throws Exception {
       return values.poll();
    }
}
