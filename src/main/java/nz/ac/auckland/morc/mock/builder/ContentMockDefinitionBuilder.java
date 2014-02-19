package nz.ac.auckland.morc.mock.builder;

/**
 * A concrete implementation of ContentMockDefinitionBuilderInit
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
