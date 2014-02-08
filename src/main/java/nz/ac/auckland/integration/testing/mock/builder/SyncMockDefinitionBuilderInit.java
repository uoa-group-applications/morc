package nz.ac.auckland.integration.testing.mock.builder;

import nz.ac.auckland.integration.testing.mock.MockDefinition;
import nz.ac.auckland.integration.testing.processor.BodyProcessor;
import nz.ac.auckland.integration.testing.processor.HeadersProcessor;
import nz.ac.auckland.integration.testing.resource.TestResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SyncMockDefinitionBuilderInit<Builder extends SyncMockDefinitionBuilderInit<Builder, T>, T>
        extends ContentMockDefinitionBuilderInit<Builder> {

    private static final Logger logger = LoggerFactory.getLogger(SyncMockDefinitionBuilderInit.class);

    private List<T> responseBodyProcessors = new ArrayList<>();
    private List<Map<String, Object>> responseHeadersProcessors = new ArrayList<>();

    public SyncMockDefinitionBuilderInit(String endpointUri) {
        super(endpointUri);
    }

    @SafeVarargs
    public final Builder responseBody(T... providedResponseBodies) {
        Collections.addAll(responseBodyProcessors, providedResponseBodies);
        return self();
    }

    /**
     * @param resources A collection of test resources that will be provided back to the caller in the body
     *                  in order they are specified in the method call
     */
    @SafeVarargs
    public final Builder responseBody(TestResource<T>... resources) {
        for (TestResource<T> resource : resources) {
            try {
                this.responseBodyProcessors.<T>add(resource.getValue());
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
    @SafeVarargs
    public final Builder responseHeaders(TestResource<Map<String, Object>>... resources) {
        for (TestResource<Map<String, Object>> resource : resources) {
            try {
                this.responseHeadersProcessors.<Map<String, Object>>add(resource.getValue());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return self();
    }

    @Override
    public MockDefinition build(MockDefinition previousDefinitionPart) {
        int responseProcessorCount = Math.max(responseBodyProcessors.size(), responseHeadersProcessors.size());

        logger.debug("{} body processors, and {} header processors provided for mock definition endpoint {}",
                new Object[]{responseBodyProcessors.size(), responseHeadersProcessors.size(), getEndpointUri()});

        for (int i = 0; i < responseProcessorCount; i++) {
            if (i < responseBodyProcessors.size())
                addProcessors(i, new BodyProcessor(responseBodyProcessors.get(i)));

            if (i < responseHeadersProcessors.size())
                addProcessors(i, new HeadersProcessor(responseHeadersProcessors.get(i)));
        }
        return super.build(previousDefinitionPart);
    }
}
