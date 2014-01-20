package nz.ac.auckland.integration.testing.mock.builder;

import nz.ac.auckland.integration.testing.mock.MockDefinition;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.cxf.binding.soap.SoapFault;

/**
 * This will cause a CXF endpoint (message consumer) to return a specified SOAP fault back to the consumer
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class SoapFaultMockDefinitionBuilder extends SyncMockDefinitionBuilder<SoapFaultMockDefinitionBuilder,SoapFault> {

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
                exchange.getOut().setFault(true);
            }
        });
        return super.build(previousDefinitionPart);
    }
}

