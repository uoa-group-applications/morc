package nz.ac.auckland.morc.specification;

import nz.ac.auckland.morc.TestBean;
import nz.ac.auckland.morc.processor.BodyProcessor;
import nz.ac.auckland.morc.processor.HeadersProcessor;
import nz.ac.auckland.morc.resource.TestResource;
import org.apache.camel.Processor;
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

    private List<TestResource<Map<String, Object>>> inputMessageHeaders = new ArrayList<>();
    private List<TestResource> inputMessageBodies = new ArrayList<>();

    /**
     * @param description The description that identifies what the test is supposed to do
     * @param endpointUri The endpoint URI of the target service under testing
     */
    public AsyncOrchestratedTestBuilder(String description, String endpointUri) {
        super(description, endpointUri);
    }

    public AsyncOrchestratedTestBuilder(String description, TestBean bean) {
        super(description, bean);
    }

    protected AsyncOrchestratedTestBuilder(String description, String endpointUri,
                                           OrchestratedTestSpecification.OrchestratedTestSpecificationBuilderInit previousPartBuilder) {
        super(description, endpointUri, previousPartBuilder);
    }

    /**
     * @param processors A list of processors to apply to the exchange before the message is sent
     */
    public AsyncOrchestratedTestBuilder input(Processor... processors) {
        this.addProcessors(processors);
        return self();
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
    public final AsyncOrchestratedTestBuilder inputHeaders(TestResource<Map<String, Object>>... resources) {
        Collections.addAll(inputMessageHeaders, resources);
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
                    addProcessors(i, new BodyProcessor(inputMessageBodies.get(i)));
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
