package nz.ac.auckland.integration.testing.mock.builder;

import nz.ac.auckland.integration.testing.mock.MockExpectation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnreceivedMockExpectationBuilder extends MockExpectation.MockExpectationBuilder<UnreceivedMockExpectationBuilder> {

    private static final Logger logger = LoggerFactory.getLogger(UnreceivedMockExpectationBuilder.class);

    public UnreceivedMockExpectationBuilder(String endpointUri) {
        super(endpointUri);
    }

    @Override
    protected UnreceivedMockExpectationBuilder self() {
        return this;
    }

    @Override
    public UnreceivedMockExpectationBuilder expectedMessageCount(int expectedMessageCount) {
        if (expectedMessageCount != 0)
            throw new IllegalArgumentException("The expected message count for an unreceived mock expectation must " +
                    "be 0");
        return super.expectedMessageCount(0);
    }

    @Override
    public MockExpectation build(MockExpectation previousExpectationPart) {
        super.expectedMessageCount(0);

        if (getPredicates().size() != 0)
            throw new IllegalStateException("No validators should be specified for an unreceived mock expectation on " +
                    "endpoint: " + getEndpointUri() + " as no messages are expected to be validated against");

        return super.build(previousExpectationPart);
    }
}
