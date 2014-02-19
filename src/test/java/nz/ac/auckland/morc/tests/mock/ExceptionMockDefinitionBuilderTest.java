package nz.ac.auckland.morc.tests.mock;

import nz.ac.auckland.morc.mock.MockDefinition;
import nz.ac.auckland.morc.mock.builder.ExceptionMockDefinitionBuilder;
import org.apache.camel.Exchange;
import org.apache.camel.component.cxf.CxfEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class ExceptionMockDefinitionBuilderTest extends Assert {

    @Test
    public void testSetCustomException() throws Exception {
        MockDefinition def = new ExceptionMockDefinitionBuilder("").exception(new IOException()).expectedMessageCount(5).build(null);
        for (int i = 0; i < 5; i++) {
            Exchange e = new DefaultExchange(new DefaultCamelContext());
            e.setFromEndpoint(new CxfEndpoint(""));
            def.getProcessors().get(i).process(e);
            assertEquals(e.getException().getClass(), IOException.class);
        }
    }

    @Test
    public void testDefaultException() throws Exception {
        MockDefinition def = new ExceptionMockDefinitionBuilder("").expectedMessageCount(5).build(null);
        for (int i = 0; i < 5; i++) {
            Exchange e = new DefaultExchange(new DefaultCamelContext());
            e.setFromEndpoint(new CxfEndpoint(""));
            def.getProcessors().get(i).process(e);
            assertEquals(e.getException().getClass(), Exception.class);
        }
    }

}
