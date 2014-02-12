package nz.ac.auckland.integration.tests.mock;

import nz.ac.auckland.integration.testing.mock.MockDefinition;
import nz.ac.auckland.integration.testing.mock.builder.SoapFaultMockDefinitionBuilder;
import org.apache.camel.Exchange;
import org.apache.camel.component.cxf.CxfEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Assert;
import org.junit.Test;

public class SoapFaultMockDefinitionBuilderTest extends Assert {

    @Test
    public void testRepeatedSetSoapFault() throws Exception {
        MockDefinition def = new SoapFaultMockDefinitionBuilder("").expectedMessageCount(5).build(null);
        for (int i = 0; i < 5; i++) {
            Exchange e = new DefaultExchange(new DefaultCamelContext());
            e.setFromEndpoint(new CxfEndpoint(""));
            def.getProcessors().get(i).process(e);
            assertTrue(e.getIn().isFault());
        }
    }
}
