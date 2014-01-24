package nz.ac.auckland.integration.testing.mock.builder;

import nz.ac.auckland.integration.testing.mock.MockDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnreceivedMockDefinitionBuilder extends MockDefinition.MockDefinitionBuilder<UnreceivedMockDefinitionBuilder> {

    private static final Logger logger = LoggerFactory.getLogger(UnreceivedMockDefinitionBuilder.class);

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

        if (getPredicates().size() != 0)
            throw new IllegalStateException("No validators should be specified for an unreceived mock definition on " +
                    "endpoint " + getEndpointUri() + " as no messages are expected to be validated against");

        return super.build(previousDefinitionPart);
    }
}
