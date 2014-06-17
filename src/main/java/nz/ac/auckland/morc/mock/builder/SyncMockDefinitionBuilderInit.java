package nz.ac.auckland.morc.mock.builder;

import nz.ac.auckland.morc.mock.MockDefinition;
import nz.ac.auckland.morc.processor.BodyProcessor;
import nz.ac.auckland.morc.processor.HeadersProcessor;
import nz.ac.auckland.morc.resource.TestResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A builder that generates a mock definition that will set the body or headers for a message response
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class SyncMockDefinitionBuilderInit<Builder extends SyncMockDefinitionBuilderInit<Builder, T>, T extends TestResource>
        extends ContentMockDefinitionBuilderInit<Builder> {

    private static final Logger logger = LoggerFactory.getLogger(SyncMockDefinitionBuilderInit.class);

    private List<T> responseBodyProcessors = new ArrayList<>();
    private List<TestResource<Map<String, Object>>> responseHeadersProcessors = new ArrayList<>();

    /**
     * @param endpointUri A Camel Endpoint URI to listen to for expected messages
     */
    public SyncMockDefinitionBuilderInit(String endpointUri) {
        super(endpointUri);
    }

    /**
     * @param resources A collection of test resources that will be provided back to the caller in the body
     *                  in order they are specified in the method call. Bodies are tied to the corresponding response
     *                  headers (if available)
     */
    @SafeVarargs
    public final Builder responseBody(T... resources) {
        Collections.addAll(responseBodyProcessors,resources);
        return self();
    }

    /**
     * @param resources A collection of test resources that will be provided back to the caller in the header
     *                  in order they are specified in the method call - headers are tied to the corresponding response
     *                  body
     */
    @SafeVarargs
    public final Builder responseHeaders(TestResource<Map<String, Object>>... resources) {
        Collections.addAll(responseHeadersProcessors,resources);
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
