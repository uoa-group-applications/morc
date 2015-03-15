package nz.ac.auckland.morc.mock.builder;

import nz.ac.auckland.morc.resource.TestResource;

/**
 * A concrete implementation of SyncMockDefinitionBuilderInit
 *
 * @author David MacDonald - d.macdonald@auckland.ac.nz
 */
public class SyncMockDefinitionBuilder extends SyncMockDefinitionBuilderInit<SyncMockDefinitionBuilder, TestResource> {
    /**
     * @param endpointUri A Camel Endpoint URI to listen to for expected messages
     */
    public SyncMockDefinitionBuilder(String endpointUri) {
        super(endpointUri);
    }
}
