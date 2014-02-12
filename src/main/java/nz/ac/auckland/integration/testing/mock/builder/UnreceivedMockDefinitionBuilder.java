package nz.ac.auckland.integration.testing.mock.builder;

import nz.ac.auckland.integration.testing.mock.MockDefinition;

/**
 * A builder to generate a mock definition that ensures no message is expected to be received at this point. This is useful
 * for setting up an endpoint to ensure no message is sent here.
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class UnreceivedMockDefinitionBuilder extends MockDefinition.MockDefinitionBuilderInit<UnreceivedMockDefinitionBuilder> {

    /**
     * @param endpointUri A Camel Endpoint URI to listen to for expected messages
     */
    public UnreceivedMockDefinitionBuilder(String endpointUri) {
        super(endpointUri);
    }

    @Override
    protected UnreceivedMockDefinitionBuilder self() {
        return this;
    }

    @Override
    public UnreceivedMockDefinitionBuilder expectedMessageCount(int expectedMessageCount) {
        if (expectedMessageCount != 0)
            throw new IllegalArgumentException("The expected message count for an unreceived mock definition on " +
                    "endpoint " + getEndpointUri() + " must be 0");
        return super.expectedMessageCount(0);
    }

    @Override
    public MockDefinition build(MockDefinition previousDefinitionPart) {
        super.expectedMessageCount(0);

        if (getPredicates().size() != 0 || getProcessors().size() != 0)
            throw new IllegalArgumentException("No processors or validators should be specified for an unreceived mock definition on " +
                    "endpoint " + getEndpointUri() + " as no messages are expected to be processed or validated against");

        return super.build(previousDefinitionPart);
    }
}
