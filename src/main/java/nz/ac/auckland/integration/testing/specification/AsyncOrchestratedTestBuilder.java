package nz.ac.auckland.integration.testing.specification;

import nz.ac.auckland.integration.testing.processor.BodyProcessor;
import nz.ac.auckland.integration.testing.processor.HeadersProcessor;
import nz.ac.auckland.integration.testing.resource.TestResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A complete test specification including all expectations for asynchronously sending a message
 * to a target destination.
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class AsyncOrchestratedTestBuilder extends OrchestratedTestSpecification.OrchestratedTestSpecificationBuilder<AsyncOrchestratedTestBuilder> {

    private static final Logger logger = LoggerFactory.getLogger(AsyncOrchestratedTestBuilder.class);

    private List<Map<String, Object>> inputMessageHeaders = new ArrayList<>();
    private List<TestResource> inputMessageBodies = new ArrayList<>();

    public AsyncOrchestratedTestBuilder(String description, String endpointUri) {
        super(description, endpointUri);
    }

    public AsyncOrchestratedTestBuilder inputMessage(TestResource... resources) {
        Collections.addAll(inputMessageBodies, resources);
        return self();
    }

    @SafeVarargs
    public final AsyncOrchestratedTestBuilder inputHeaders(Map<String, Object>... resources) {
        Collections.addAll(inputMessageHeaders, resources);
        return self();
    }

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
    public OrchestratedTestSpecification build() {
        if (getMockExpectations().size() == 0)
            throw new IllegalArgumentException("At least 1 mock expectation must be set for the " +
                    "asynchronous test specification " + getDescription());

        logger.info("The endpoint {} will be sending {} input message bodies and {} input message headers",
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

        return super.build();
    }

}
