package nz.ac.auckland.integration.testing.mock.builder;

import nz.ac.auckland.integration.testing.mock.MockExpectation;
import nz.ac.auckland.integration.testing.processor.ResponseBodiesProcessor;
import nz.ac.auckland.integration.testing.processor.ResponseHeadersProcessor;
import nz.ac.auckland.integration.testing.resource.TestResource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SyncMockExpectationBuilder<Builder extends SyncMockExpectationBuilder<Builder, T>, T>
        extends ContentMockExpectationBuilder<Builder> {

    private List<T> responseBodyProcessors = new ArrayList<>();
    private List<Map<String, Object>> responseHeadersProcessors = new ArrayList<>();
    private Class<? extends ResponseBodiesProcessor> responseBodiesProcessor = ResponseBodiesProcessor.class;
    private Class<? extends ResponseHeadersProcessor> responseHeadersProcessor = ResponseHeadersProcessor.class;
    private boolean matchedResponses = false;

    public SyncMockExpectationBuilder(String endpointUri) {
        super(endpointUri);
    }

    /**
     * @param providedResponseBody The body that should be returned back to the client
     */
    @SafeVarargs
    public final Builder responseBody(T... providedResponseBody) {
        this.responseBodyProcessors.addAll(Arrays.asList(providedResponseBody));
        return self();
    }

    /**
     * @param resources A collection of test resources that will be provided back to the caller in the body
     *                  in order they are specified in the method call
     */
    @SuppressWarnings("unchecked")
    public final Builder responseBody(TestResource<T>... resources) {
        for (TestResource<T> resource : resources) {
            try {
                this.responseBodyProcessors.add(resource.getValue());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return self();
    }

    /**
     * @param providedResponseHeaders The headers that should be returned back to the client
     */
    @SafeVarargs
    public final Builder responseHeaders(Map<String, Object>... providedResponseHeaders) {
        this.responseHeadersProcessors.addAll(Arrays.asList(providedResponseHeaders));
        return self();
    }

    /**
     * @param resources A collection of test resources that will be provided back to the caller in the header
     *                  in order they are specified in the method call
     */
    @SuppressWarnings("unchecked")
    public Builder responseHeaders(TestResource<Map<String, Object>>... resources) {

        for (TestResource<Map<String, Object>> resource : resources) {
            try {
                this.responseHeadersProcessors.add(resource.getValue());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return self();
    }

    public Builder responseBodyProcessor(Class<? extends ResponseBodiesProcessor> clazz) {
        this.responseBodiesProcessor = clazz;
        return self();
    }

    public Builder responseHeadersProcessor(Class<? extends ResponseHeadersProcessor> clazz) {
        this.responseHeadersProcessor = clazz;
        return self();
    }

    @Override
    public MockExpectation build(MockExpectation previousExpectationPart, int index) {

        int responseProcessorCount = Math.max(responseBodyProcessors.size(), responseHeadersProcessors.size());

        //todo add .matchedResponses() (warn if totally ordered)

        ResponseBodiesProcessor bodiesProcessor;
        ResponseHeadersProcessor headersProcessor;

        try {
            bodiesProcessor = responseBodiesProcessor.newInstance();
            headersProcessor = responseHeadersProcessor.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        for (int i = 0; i < responseProcessorCount; i++) {
            if (i < responseBodyProcessors.size())
                bodiesProcessor.addResponseBody(responseBodyProcessors.get(i));

            if (i < responseHeadersProcessors.size())
                headersProcessor.addResponseHeaders(responseHeadersProcessors.get(i));
        }

        this.addRepeatedProcessor(bodiesProcessor);
        this.addRepeatedProcessor(headersProcessor);

        return super.build(previousExpectationPart, index);
    }
}
