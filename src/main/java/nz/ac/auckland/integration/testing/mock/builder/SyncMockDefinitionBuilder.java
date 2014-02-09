package nz.ac.auckland.integration.testing.mock.builder;

/**
 * A concrete implementation of SyncMockDefinitionBuilderInit
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class SyncMockDefinitionBuilder extends SyncMockDefinitionBuilderInit<SyncMockDefinitionBuilder, Object> {
    /**
     * @param endpointUri A Camel Endpoint URI to listen to for expected messages
     */
    public SyncMockDefinitionBuilder(String endpointUri) {
        super(endpointUri);
    }
}
