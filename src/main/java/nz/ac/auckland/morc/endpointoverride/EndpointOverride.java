package nz.ac.auckland.morc.endpointoverride;

import org.apache.camel.Endpoint;

/**
 * An interface for setting endpoint parameters each time rather than specifying them
 * for each test specification, or overriding Camel defaults
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public interface EndpointOverride {

    public void overrideEndpoint(Endpoint endpoint);
}
