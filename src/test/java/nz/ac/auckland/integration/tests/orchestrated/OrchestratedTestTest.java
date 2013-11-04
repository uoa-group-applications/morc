package nz.ac.auckland.integration.tests.orchestrated;

import nz.ac.auckland.integration.testing.OrchestratedTest;
import nz.ac.auckland.integration.testing.endpointoverride.CxfEndpointOverride;
import nz.ac.auckland.integration.testing.endpointoverride.EndpointOverride;
import org.apache.camel.Endpoint;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class OrchestratedTestTest extends Assert {

    @Test
    public void testSpringContextPathsConstructor() throws Exception {
        String[] inputs = new String[] {"foo.xml","baz.xml"};
        OrchestratedTest test = new OrchestratedTest(null,inputs);
        assertArrayEquals(inputs, test.getSpringContextPaths());
    }

    @Test
    public void testPropertiesConstructor() throws Exception {
        OrchestratedTest test = new OrchestratedTest(null,"foo.properties");
        assertEquals("foo.properties",test.getPropertiesLocation());
    }

    @Test
    public void testSpringContextPropertiesConstructor() throws Exception {
        String[] inputs = new String[] {"foo.xml","baz.xml"};
        OrchestratedTest test = new OrchestratedTest(null,inputs,"foo.properties");
        assertArrayEquals(inputs, test.getSpringContextPaths());
        assertEquals("foo.properties",test.getPropertiesLocation());
    }

    @Test
    public void testDefaultEndpointOverrides() throws Exception {
        OrchestratedTest test = new OrchestratedTest(null);
        assertEquals(1, test.getEndpointOverrides().size());
        assertTrue(test.getEndpointOverrides().iterator().next() instanceof CxfEndpointOverride);
    }

    @Test
    public void testSetEndpointOverrides() throws Exception {
        Collection<EndpointOverride> endpointOverrides = new ArrayList<>();
        endpointOverrides.add(new EndpointOverride() {
            @Override
            public void overrideEndpoint(Endpoint endpoint) {
                throw new RuntimeException(new IOException());
            }
        });

        OrchestratedTest test = new OrchestratedTest(null);
        test.setEndpointOverrides(endpointOverrides);

        assertEquals(1,test.getEndpointOverrides().size());

        Exception ex = null;
        try {
            test.getEndpointOverrides().iterator().next().overrideEndpoint(null);
        } catch (Exception e) {
            ex = e;
        }

        assertNotNull(ex);
        assertTrue(ex.getCause() instanceof IOException);
    }
}
