package nz.ac.auckland.integration.testing.mock.builder;

import nz.ac.auckland.integration.testing.mock.MockDefinition;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.cxf.binding.soap.SoapFault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This will cause a CXF endpoint (message consumer) to return a specified SOAP fault back to the consumer
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class SoapFaultMockDefinitionBuilder extends SyncMockDefinitionBuilderInit<SoapFaultMockDefinitionBuilder, SoapFault> {

    private static final Logger logger = LoggerFactory.getLogger(SoapFaultMockDefinitionBuilder.class);

    /**
     * @param endpointUri This MUST be a CXF endpoint URI
     */
    public SoapFaultMockDefinitionBuilder(String endpointUri) {
        super(endpointUri);
    }

    @Override
    public MockDefinition build(MockDefinition previousDefinitionPart) {
        addRepeatedProcessor(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                logger.trace("Setting fault for exchange arriving from endpoint {}", exchange.getFromEndpoint().getEndpointUri());
                exchange.getOut().setFault(true);
            }
        });
        return super.build(previousDefinitionPart);
    }
}

