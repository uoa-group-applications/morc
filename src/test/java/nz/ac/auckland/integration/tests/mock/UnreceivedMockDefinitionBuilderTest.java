package nz.ac.auckland.integration.tests.mock;

import nz.ac.auckland.integration.testing.mock.builder.UnreceivedMockDefinitionBuilder;
import org.junit.Assert;
import org.junit.Test;

import static nz.ac.auckland.integration.testing.MorcTestBuilder.*;

public class UnreceivedMockDefinitionBuilderTest extends Assert {

    @Test
    public void testSpecifyPositiveReceived() throws Exception {
        IllegalArgumentException e = null;
        try {
            new UnreceivedMockDefinitionBuilder("").expectedMessageCount(5);
        } catch (IllegalArgumentException ex) {
            e = ex;
        }

        assertNotNull(e);
    }

    @Test
    public void testSpecifyPredicates() throws Exception {
        IllegalArgumentException e = null;
        try {
            new UnreceivedMockDefinitionBuilder("")
                    .addPredicates(text("1")).build(null);
        } catch (IllegalArgumentException ex) {
            e = ex;
        }

        assertNotNull(e);
    }

}
