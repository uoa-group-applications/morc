package nz.ac.auckland.integration.testing.mock.builder;

import nz.ac.auckland.integration.testing.mock.MockDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An expectation that an asynchronous message will be received at some point
 * in the future.
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class AsyncMockDefinitionBuilder extends ContentMockDefinitionBuilder {

    private static final Logger logger = LoggerFactory.getLogger(AsyncMockDefinitionBuilder.class);

    public AsyncMockDefinitionBuilder(String endpointUri) {
        super(endpointUri);
    }

    @Override
    public MockDefinition build(MockDefinition previousDefinitionPart) {
        if (getOrderingType() == MockDefinition.OrderingType.TOTAL) {
            logger.warn("The asynchronous mock definition endpoint {} used total ordering, this was changed to partial ordering",
                    getEndpointUri());
            ordering(MockDefinition.OrderingType.PARTIAL);
        }
        return super.build(previousDefinitionPart);
    }
}
