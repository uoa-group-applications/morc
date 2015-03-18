package nz.ac.auckland.morc.mock.builder;

import nz.ac.auckland.morc.mock.MockDefinition;
import org.apache.camel.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A builder to generate a mock definition that validates the bodies and headers for an incoming message
 *
 * @author David MacDonald - d.macdonald@auckland.ac.nz
 */
public class ContentMockDefinitionBuilderInit<Builder extends ContentMockDefinitionBuilderInit<Builder>>
        extends MockDefinition.MockDefinitionBuilderInit<Builder> {

    private static final Logger logger = LoggerFactory.getLogger(ContentMockDefinitionBuilderInit.class);

    @SuppressWarnings("unchecked")
    @Override
    protected Builder self() {
        return (Builder) this;
    }

    /**
     * @param endpointUri A Camel Endpoint URI to listen to for expected messages
     */
    public ContentMockDefinitionBuilderInit(String endpointUri) {
        super(endpointUri);
    }

    /**
     * @param validators A list of validators that check the body is as expected; each validator will match
     *                   to a headers validator if available at a particular index
     */
    public Builder expectation(Predicate... validators) {
        return addPredicates(validators);
    }

    /**
     * Expect a repeat of the same expectation multiple times
     *
     * @param count      The number of times to repeat these predicates (separate responses)
     * @param validators The set of response validators/predicates that will be used to validate consecutive responses
     */
    public Builder expectationMultiplier(int count, Predicate... validators) {
        return predicateMultiplier(count, validators);
    }
}
