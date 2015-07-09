package nz.ac.auckland.morc.specification;

import nz.ac.auckland.morc.TestBean;
import nz.ac.auckland.morc.processor.HttpExceptionResponseProcessor;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;

/**
 * A builder that generates a synchronous orchestrated test specification that will call a target endpoint
 * that provides a response(s). During the request process the target may make a number of call outs to expectations
 * which need to be satisfied. The response bodies from the target will also be validated against the expected
 * response bodies.
 *
 * @author David MacDonald - d.macdonald@auckland.ac.nz
 */
public class SyncOrchestratedTestBuilder extends OrchestratedTestSpecification.OrchestratedTestSpecificationBuilderInit<SyncOrchestratedTestBuilder> {

    /**
     * @param description The description that identifies what the test is supposed to do
     * @param endpointUri The endpoint URI of the target service under testing
     */
    public SyncOrchestratedTestBuilder(String description, String endpointUri) {
        super(description, endpointUri);
    }

    public SyncOrchestratedTestBuilder(String description, TestBean bean) {
        super(description, bean);
    }

    protected SyncOrchestratedTestBuilder(String description, String endpointUri,
                                          OrchestratedTestSpecification.OrchestratedTestSpecificationBuilderInit previousPartBuilder) {
        super(description, endpointUri, previousPartBuilder);
        //In the case of HTTP responses we need to rip out the body and headers and put it in the message for proper
        //validation
        addMockFeedPreprocessor(new HttpExceptionResponseProcessor());
    }

    /**
     * @param processors A collection of processors that will be applied to an exchange before it is sent
     */
    public SyncOrchestratedTestBuilder request(Processor... processors) {
        return addProcessors(processors);
    }

    /**
     * Replay the same request for the specified number of times
     *
     * @param count      The number of times to repeat these processors (separate requests)
     * @param processors A collection of processors that will be applied to an exchange before it is sent
     */
    public SyncOrchestratedTestBuilder requestMultiplier(int count, Processor... processors) {
        return processorMultiplier(count, processors);
    }

    /**
     * @param predicates The set of response validators/predicates that will be used to validate consecutive responses
     */
    public SyncOrchestratedTestBuilder expectation(Predicate... predicates) {
        return addPredicates(predicates);
    }

    /**
     * Expect a repeat of the same predicates multiple times
     *
     * @param count      The number of times to repeat these predicates (separate responses)
     * @param predicates The set of response validators/predicates that will be used to validate consecutive responses
     */
    public SyncOrchestratedTestBuilder expectationMultiplier(int count, Predicate predicates) {
        return predicateMultiplier(count, predicates);
    }

    @Override
    public OrchestratedTestSpecification build(int partCount, OrchestratedTestSpecification nextPart) {
        addRepeatedProcessor(exchange -> exchange.setPattern(ExchangePattern.InOut));

        return super.build(partCount, nextPart);
    }
}
