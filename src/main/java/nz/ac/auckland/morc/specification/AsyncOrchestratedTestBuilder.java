package nz.ac.auckland.morc.specification;

import nz.ac.auckland.morc.TestBean;
import org.apache.camel.Processor;

/**
 * A builder that generates a complete test specification including all expectations for asynchronously sending a message
 * to a target destination.
 *
 * @author David MacDonald - d.macdonald@auckland.ac.nz
 */
public class AsyncOrchestratedTestBuilder extends OrchestratedTestSpecification.OrchestratedTestSpecificationBuilderInit<AsyncOrchestratedTestBuilder> {

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
        return addProcessors(processors);
    }

    /**
     * Replay the same request for the specified number of times
     *
     * @param count      The number of times to repeat these processors (separate requests)
     * @param processors A collection of processors that will be applied to an exchange before it is sent
     */
    public AsyncOrchestratedTestBuilder inputMultiplier(int count, Processor... processors) {
        return processorMultiplier(count, processors);
    }
}
