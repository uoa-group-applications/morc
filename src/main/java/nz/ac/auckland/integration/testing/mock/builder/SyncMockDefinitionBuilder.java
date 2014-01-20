package nz.ac.auckland.integration.testing.mock.builder;

import nz.ac.auckland.integration.testing.mock.MockDefinition;
import nz.ac.auckland.integration.testing.processor.MatchedResponseBodiesProcessor;
import nz.ac.auckland.integration.testing.processor.ResponseBodyProcessor;
import nz.ac.auckland.integration.testing.processor.ResponseHeadersProcessor;
import nz.ac.auckland.integration.testing.resource.TestResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SyncMockDefinitionBuilder<Builder extends SyncMockDefinitionBuilder<Builder, T>, T>
        extends ContentMockDefinitionBuilder<Builder> {

    private static final Logger logger = LoggerFactory.getLogger(SyncMockDefinitionBuilder.class);

    private List<T> responseBodyProcessors = new ArrayList<>();
    private List<Map<String, Object>> responseHeadersProcessors = new ArrayList<>();
    private boolean matchedResponses = false;

    public SyncMockDefinitionBuilder(String endpointUri) {
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

    public Builder matchedResponses() {
        matchedResponses = true;
        return self();
    }

    @Override
    public MockDefinition build(MockDefinition previousDefinitionPart) {

        int responseProcessorCount = Math.max(responseBodyProcessors.size(), responseHeadersProcessors.size());

        MatchedResponseBodiesProcessor matchedResponseBodiesProcessor = new MatchedResponseBodiesProcessor();

        if (matchedResponses)

            for (int i = 0; i < responseProcessorCount; i++) {

                if (i < responseBodyProcessors.size())
                    addProcessors(i, new ResponseBodyProcessor(responseBodyProcessors.get(i)));

                if (i < responseHeadersProcessors.size())
                    addProcessors(i, new ResponseHeadersProcessor(responseHeadersProcessors.get(i)));
            }

        return super.build(previousDefinitionPart);
    }
}
