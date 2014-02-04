package nz.ac.auckland.integration.testing.mock.builder;

/**
 * A class to set expectations for bodies and headers for
 * an incoming message
 * <p/>
 * This class carries out the message validation based on the test resource, expectations
 * will be returned in the order specified even if some relaxation of total ordering occurs
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class ContentMockDefinitionBuilder extends ContentMockDefinitionBuilderInit<ContentMockDefinitionBuilder> {

    /**
     * @param endpointUri A Camel Endpoint URI to listen to for expected messages
     */
    public ContentMockDefinitionBuilder(String endpointUri) {
        super(endpointUri);
    }

}
