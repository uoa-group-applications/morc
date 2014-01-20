package nz.ac.auckland.integration.testing.mock.builder;

import nz.ac.auckland.integration.testing.mock.MockDefinition;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * This will cause a Jetty or CXF endpoint (message consumer) to throw an HTTP 500 (or other)
 * error with the specified body - this could be a SOAP fault message in the case of a web-service
 * <p/>
 * It is recommended to use SoapFaultMockDefinitionBuilder if you want to throw correct CXF SOAP Faults
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class HttpErrorMockDefinitionBuilder extends SyncMockDefinitionBuilder<HttpErrorMockDefinitionBuilder, Processor> {

    private int statusCode = 500;

    public HttpErrorMockDefinitionBuilder(String endpointUri) {
        super(endpointUri);
    }

    /**
     * @param statusCode The HTTP response code to send back to the client, defaults to 500
     */
    public HttpErrorMockDefinitionBuilder statusCode(int statusCode) {
        this.statusCode = statusCode;
        return self();
    }

    @Override
    public MockDefinition build(MockDefinition previousDefinitionPart) {
        this.addRepeatedProcessor(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, statusCode);
            }
        });

        return super.build(previousDefinitionPart);
    }

}
