package nz.ac.auckland.integration.testing.endpointoverride;

import org.apache.camel.Endpoint;
import org.apache.camel.component.cxf.CxfEndpoint;
import org.apache.camel.component.cxf.DataFormat;

/**
 * Overrides the default CXF endpoint dataFormat of POJO to PAYLOAD as we're not
 * interested in reading the data as a bean
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class CxfEndpointOverride implements EndpointOverride {

    @Override
    public void overrideEndpoint(Endpoint endpoint) {
        if (endpoint instanceof CxfEndpoint) {
            ((CxfEndpoint) endpoint).setDataFormat(DataFormat.PAYLOAD);
            //Works around issue: https://issues.apache.org/jira/browse/CXF-2775
            System.setProperty("org.apache.cxf.transports.http_jetty.DontClosePort","true");
        }
    }
}
