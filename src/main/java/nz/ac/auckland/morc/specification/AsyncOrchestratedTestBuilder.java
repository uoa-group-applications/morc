package nz.ac.auckland.morc.specification;

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
 * A builder that generates a complete test specification including all expectations for asynchronously sending a message
 * to a target destination.
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class AsyncOrchestratedTestBuilder extends OrchestratedTestSpecification.OrchestratedTestSpecificationBuilderInit<AsyncOrchestratedTestBuilder> {

    private static final Logger logger = LoggerFactory.getLogger(AsyncOrchestratedTestBuilder.class);

    private List<Map<String, Object>> inputMessageHeaders = new ArrayList<>();
    private List<TestResource> inputMessageBodies = new ArrayList<>();

    /**
     * @param description The description that identifies what the test is supposed to do
     * @param endpointUri The endpoint URI of the target service under testing
     */
    public AsyncOrchestratedTestBuilder(String description, String endpointUri) {
        super(description, endpointUri);
    }

    protected AsyncOrchestratedTestBuilder(String description, String endpointUri,
                                           OrchestratedTestSpecification.OrchestratedTestSpecificationBuilderInit previousPartBuilder) {
        super(description, endpointUri, previousPartBuilder);
    }

    /**
     * @param resources The set of resources that should be sent in the body to the target endpoint URI - these will
     *                  match to the corresponding inputHeaders if available
     */
    public AsyncOrchestratedTestBuilder inputMessage(TestResource... resources) {
        Collections.addAll(inputMessageBodies, resources);
        return self();
    }

    /**
     * @param resources The set of resources that should be sent as headers to the target endpoint URI - these will
     *                  match to the corresponding inputMessage if available
     */
    @SafeVarargs
    public final AsyncOrchestratedTestBuilder inputHeaders(Map<String, Object>... resources) {
        Collections.addAll(inputMessageHeaders, resources);
        return self();
    }

    /**
     * @param resources The set of resources that should be sent as headers to the target endpoint URI - these will
     *                  match to the corresponding inputMessage if available
     */
    @SafeVarargs
    public final AsyncOrchestratedTestBuilder inputHeaders(TestResource<Map<String, Object>>... resources) {
        for (TestResource<Map<String, Object>> resource : resources) {
            try {
                inputMessageHeaders.add(resource.getValue());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return self();
    }

    /**
     * @throws IllegalArgumentException if no expectations are specified
     */
    public OrchestratedTestSpecification build(int partCount, OrchestratedTestSpecification nextPart) {
        logger.debug("The endpoint {} will receive {} input message bodies and {} input message headers",
                new Object[]{getEndpointUri(), inputMessageBodies.size(), inputMessageHeaders.size()});

        int messageCount = Math.max(inputMessageBodies.size(), inputMessageHeaders.size());

        for (int i = 0; i < messageCount; i++) {
            if (i < inputMessageBodies.size()) {
                try {
                    addProcessors(i, new BodyProcessor(inputMessageBodies.get(i).getValue()));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            if (i < inputMessageHeaders.size())
                addProcessors(i, new HeadersProcessor(inputMessageHeaders.get(i)));
        }

        return super.build(partCount, nextPart);
    }

}
