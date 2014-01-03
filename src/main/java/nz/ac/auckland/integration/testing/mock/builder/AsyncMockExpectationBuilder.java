package nz.ac.auckland.integration.testing.mock.builder;

import nz.ac.auckland.integration.testing.mock.MockExpectation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An expectation that an asynchronous message will be received at some point
 * in the future.
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class AsyncMockExpectationBuilder extends ContentMockExpectationBuilder {

    private static final Logger logger = LoggerFactory.getLogger(AsyncMockExpectationBuilder.class);

    public AsyncMockExpectationBuilder(String endpointUri) {
        super(endpointUri);
    }

    @Override
    public MockExpectation build(MockExpectation previousExpectationPart, int index) {
        if (getOrderingType() == MockExpectation.OrderingType.TOTAL) {
            logger.warn("The asynchronous endpoint {} used total ordering, this was changed to partial ordering",
                    getEndpointUri());
            ordering(MockExpectation.OrderingType.PARTIAL);
        }
        return super.build(previousExpectationPart,index);
    }
}
