package nz.ac.auckland.morc.endpointoverride;

import org.apache.camel.Endpoint;

/**
 * A simple class to ensure that HTTP connections are torn down between tests
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class UrlConnectionOverride implements EndpointOverride {
    @Override
    public void overrideEndpoint(Endpoint endpoint) {
        //Saving connections between tests causes a lot of problems
        System.setProperty("http.keepAlive", "false");
    }
}
