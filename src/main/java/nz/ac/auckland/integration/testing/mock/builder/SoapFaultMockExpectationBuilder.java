package nz.ac.auckland.integration.testing.mock.builder;

import nz.ac.auckland.integration.testing.mock.MockExpectation;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.cxf.binding.soap.SoapFault;

/**
 * This will cause a CXF endpoint (message consumer) to return a specified SOAP fault back to the consumer
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class SoapFaultMockExpectationBuilder extends SyncMockExpectationBuilder<SoapFaultMockExpectationBuilder,SoapFault> {

    /**
     * @param endpointUri This MUST be a CXF endpoint URI
     */
    public SoapFaultMockExpectationBuilder(String endpointUri) {
        super(endpointUri);
    }

    @Override
    public MockExpectation build(MockExpectation previousExpectationPart) {
        addRepeatedProcessor(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                exchange.getOut().setFault(true);
            }
        });
        return super.build(previousExpectationPart);
    }
}

