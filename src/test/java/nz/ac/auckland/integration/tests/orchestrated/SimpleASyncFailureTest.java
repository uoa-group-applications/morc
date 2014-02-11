package nz.ac.auckland.integration.tests.orchestrated;

import nz.ac.auckland.integration.testing.specification.AsyncOrchestratedTestBuilder;
import nz.ac.auckland.integration.testing.specification.OrchestratedTestSpecification;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static nz.ac.auckland.integration.testing.MorcTestBuilder.*;

public class SimpleAsyncFailureTest extends CamelTestSupport {

    @Test
    public void testDelayedDeliveryFails() throws Exception {
        Exception e = null;

        try {
            OrchestratedTestSpecification spec = new AsyncOrchestratedTestBuilder("Test no expectations configured",
                "vm:async")
                .inputMessage(text("foo"))
                .build();
        } catch (Exception ex) {
            e = ex;
        }

        assertNotNull(e);
        assertEquals(IllegalArgumentException.class,e.getClass());
    }
}