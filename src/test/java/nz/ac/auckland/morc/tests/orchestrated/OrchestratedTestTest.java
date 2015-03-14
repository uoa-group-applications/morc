package nz.ac.auckland.morc.tests.orchestrated;

import nz.ac.auckland.morc.MorcTest;
import org.junit.Assert;
import org.junit.Test;

public class OrchestratedTestTest extends Assert {

    @Test
    public void testSpringContextPathsConstructor() throws Exception {
        String[] inputs = new String[]{"foo.xml", "baz.xml"};
        MorcTest test = new MorcTest(null, inputs);
        assertArrayEquals(inputs, test.getSpringContextPaths());
    }

    @Test
    public void testPropertiesConstructor() throws Exception {
        MorcTest test = new MorcTest(null, "foo.properties");
        assertEquals("foo.properties", test.getPropertiesLocation());
    }

    @Test
    public void testSpringContextPropertiesConstructor() throws Exception {
        String[] inputs = new String[]{"1.xml", "baz.xml"};
        MorcTest test = new MorcTest(null, inputs, "foo.properties");
        assertArrayEquals(inputs, test.getSpringContextPaths());
        assertEquals("foo.properties", test.getPropertiesLocation());
    }

}
