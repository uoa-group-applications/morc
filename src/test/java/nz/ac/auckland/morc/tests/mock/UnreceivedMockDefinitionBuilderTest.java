package nz.ac.auckland.morc.tests.mock;

import nz.ac.auckland.morc.mock.builder.UnreceivedMockDefinitionBuilder;
import nz.ac.auckland.morc.processor.BodyProcessor;
import org.junit.Assert;
import org.junit.Test;

import static nz.ac.auckland.morc.MorcTestBuilder.text;

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

    @Test
    public void testSpecifyProcessors() throws Exception {
        IllegalArgumentException e = null;
        try {
            new UnreceivedMockDefinitionBuilder("")
                    .addProcessors(new BodyProcessor(text("1"))).build(null);
        } catch (IllegalArgumentException ex) {
            e = ex;
        }

        assertNotNull(e);
    }

}
