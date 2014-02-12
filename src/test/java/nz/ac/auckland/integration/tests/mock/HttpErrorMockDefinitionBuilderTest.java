package nz.ac.auckland.integration.tests.mock;

import nz.ac.auckland.integration.testing.mock.MockDefinition;
import nz.ac.auckland.integration.testing.mock.builder.HttpErrorMockDefinitionBuilder;
import nz.ac.auckland.integration.testing.mock.builder.SoapFaultMockDefinitionBuilder;
import org.apache.camel.Exchange;
import org.apache.camel.component.cxf.CxfEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Assert;
import org.junit.Test;

public class HttpErrorMockDefinitionBuilderTest extends Assert {

    @Test
    public void testRepeatedSetHttpCode() throws Exception {
        MockDefinition def = new HttpErrorMockDefinitionBuilder("").expectedMessageCount(5).statusCode(505).build(null);
        for (int i = 0; i < 5; i++) {
            Exchange e = new DefaultExchange(new DefaultCamelContext());
            e.setFromEndpoint(new CxfEndpoint(""));
            def.getProcessors().get(i).process(e);
            assertEquals(e.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE,Integer.class),new Integer(505));
        }
    }
}
