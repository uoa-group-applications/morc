package nz.ac.auckland.integration.tests.expectation;

import nz.ac.auckland.integration.testing.expectation.HttpErrorMockExpectation;
import nz.ac.auckland.integration.testing.resource.XmlTestResource;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Assert;
import org.junit.Test;

public class CxfFaultMockExpectationTest extends Assert {

    @Test
    public void testBodyAndHeadersSetCorrect() throws Exception {
        CamelContext ctx = new DefaultCamelContext();
        Exchange exchange = new DefaultExchange(ctx);

        XmlTestResource output = new XmlTestResource(this.getClass().getResource("/data/cxf-error-response1.xml"));
        HttpErrorMockExpectation mockTest = new HttpErrorMockExpectation.Builder("vm:test")
                .responseBody(output).build();

        mockTest.handleReceivedExchange(exchange);

        assertEquals(output.getValue(), exchange.getOut().getBody());
        assertEquals(exchange.getOut().getHeader("org.apache.cxf.message.Message.RESPONSE_CODE"), 500);
    }

}
