package nz.ac.auckland.morc.mock.builder;

import nz.ac.auckland.morc.mock.MockDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates an mock definition for ensuring asynchronous messages will be received at some point in the future.
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class AsyncMockDefinitionBuilder extends ContentMockDefinitionBuilderInit<AsyncMockDefinitionBuilder> {

    private static final Logger logger = LoggerFactory.getLogger(AsyncMockDefinitionBuilder.class);

    /**
     * @param endpointUri A Camel Endpoint URI to listen to for expected messages
     */
    public AsyncMockDefinitionBuilder(String endpointUri) {
        super(endpointUri);
        ordering(MockDefinition.OrderingType.PARTIAL);
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
