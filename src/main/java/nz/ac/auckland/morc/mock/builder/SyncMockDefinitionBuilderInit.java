package nz.ac.auckland.morc.mock.builder;

import org.apache.camel.Processor;

/**
 * A builder that generates a mock definition that will set the body or headers for a message response
 *
 * @author David MacDonald - d.macdonald@auckland.ac.nz
 */
public class SyncMockDefinitionBuilderInit<Builder extends SyncMockDefinitionBuilderInit<Builder>>
        extends ContentMockDefinitionBuilderInit<Builder> {
    /**
     * @param endpointUri A Camel Endpoint URI to listen to for expected messages
     */
    public SyncMockDefinitionBuilderInit(String endpointUri) {
        super(endpointUri);
    }

    /**
     * @param processors A list of processors to apply to the response exchange
     */
    public final Builder response(Processor... processors) {
        return addProcessors(processors);
    }

    /**
     * Replay the same request for the specified number of times
     *
     * @param count      The number of times to repeat these processors (separate requests)
     * @param processors A collection of processors that will be applied to an exchange before it is sent
     */
    public Builder responseMultiplier(int count, Processor... processors) {
        return processorMultiplier(count, processors);
    }
}
