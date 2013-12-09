package nz.ac.auckland.integration.testing.endpointoverride;

import org.apache.camel.Endpoint;

public class UrlConnectionOverride implements EndpointOverride {
    @Override
    public void overrideEndpoint(Endpoint endpoint) {
        //Saving connections between tests causes a lot of problems
        System.setProperty("http.keepAlive", "false");
    }
}
