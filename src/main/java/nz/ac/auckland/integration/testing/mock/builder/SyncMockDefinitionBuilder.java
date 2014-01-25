package nz.ac.auckland.integration.testing.mock.builder;

import nz.ac.auckland.integration.testing.mock.MockDefinition;
import nz.ac.auckland.integration.testing.processor.BodyProcessor;
import nz.ac.auckland.integration.testing.processor.HeadersProcessor;
import nz.ac.auckland.integration.testing.resource.TestResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class SyncMockDefinitionBuilder<Builder extends SyncMockDefinitionBuilder<Builder, T>, T>
        extends ContentMockDefinitionBuilder<Builder> {

    private static final Logger logger = LoggerFactory.getLogger(SyncMockDefinitionBuilder.class);

    private List<T> responseBodyProcessors = new ArrayList<>();
    private List<Map<String, Object>> responseHeadersProcessors = new ArrayList<>();

    public SyncMockDefinitionBuilder(String endpointUri) {
        super(endpointUri);
    }

    @SafeVarargs
    public final Builder responseBody(T... providedResponseBodies) {
        Collections.addAll(responseBodyProcessors,providedResponseBodies);
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
        Collections.addAll(responseHeadersProcessors, providedResponseHeaders);
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

    @Override
    public MockDefinition build(MockDefinition previousDefinitionPart) {
        int responseProcessorCount = Math.max(responseBodyProcessors.size(), responseHeadersProcessors.size());

        for (int i = 0; i < responseProcessorCount; i++) {
            if (i < responseBodyProcessors.size())
                addProcessors(i, new BodyProcessor(responseBodyProcessors.get(i)));

            if (i < responseHeadersProcessors.size())
                addProcessors(i, new HeadersProcessor(responseHeadersProcessors.get(i)));
        }
        return super.build(previousDefinitionPart);
    }
}
