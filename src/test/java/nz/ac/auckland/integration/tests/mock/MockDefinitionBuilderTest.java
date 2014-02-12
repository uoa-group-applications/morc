package nz.ac.auckland.integration.tests.mock;

import nz.ac.auckland.integration.testing.mock.MockDefinition;
import nz.ac.auckland.integration.testing.processor.BodyProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.model.RouteDefinition;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static nz.ac.auckland.integration.testing.MorcTestBuilder.*;

public class MockDefinitionBuilderTest extends Assert {

    @Test
    public void testNegativeExpectedMessageCount() throws Exception {
        IllegalArgumentException e = null;
        try {
            new MockDefinition.MockDefinitionBuilder("").expectedMessageCount(-1).build(null);
        } catch (IllegalArgumentException ex) {
            e = ex;
        }

        assertNotNull(e);
    }

    @Test
    public void testLenientSelectorWithExpectedMessages() throws Exception {
        MockDefinition def = new MockDefinition.MockDefinitionBuilder("").addProcessors(new BodyProcessor(text("foo")),
                new BodyProcessor(text("baz"))).addPredicates(text("foo"), text("baz")).lenient().build(null);

        assertEquals(0,def.getPredicates().size());
        assertEquals(0,def.getProcessors().size());
        assertNotNull(def.getLenientSelector());
        assertEquals(def.getLenientProcessor().getClass(),MockDefinition.LenientProcessor.class);
    }

    @Test
    public void testCustomLenientSelectorWithExpectedMessages() throws Exception {
        Predicate predicate = new Predicate() {
            @Override
            public boolean matches(Exchange exchange) {
                return true;
            }
        };

        //todo improve processor implementation
        MockDefinition def = new MockDefinition.MockDefinitionBuilder("").addProcessors(new BodyProcessor(text("foo")),
                new BodyProcessor(text("baz"))).addPredicates(text("foo"),text("baz")).lenient(predicate)
                .lenientProcessor(StubLenientProcessor.class).build(null);

        assertEquals(0,def.getPredicates().size());
        assertEquals(0,def.getProcessors().size());
        assertEquals(predicate, def.getLenientSelector());
        assertEquals(def.getLenientProcessor().getClass(),StubLenientProcessor.class);
    }

    @Test
    public void testMockFeedRoute() throws Exception {
        RouteDefinition rd = new RouteDefinition();
        rd.setGroup("foo");

        MockDefinition def = new MockDefinition.MockDefinitionBuilder("").addProcessors(new BodyProcessor(text("foo")),
                new BodyProcessor(text("baz"))).addPredicates(text("foo"), text("baz"))
                .mockFeederRoute(rd).build(null);

        assertEquals("foo",def.getMockFeederRoute().getGroup());
    }

    @Test
    public void testMergeMockFeederRoute() throws Exception {

    }

    @Test
    public void testMergeOrdering() throws Exception {
        //endpoint
        //overall
    }

    @Test
    public void testMergeFailWrongUri() throws Exception {
        MockDefinition def = new MockDefinition.MockDefinitionBuilder("foo").build(null);

        IllegalArgumentException e = null;
        try {
            MockDefinition def1 = new MockDefinition.MockDefinitionBuilder("baz").build(def);
        } catch (IllegalArgumentException ex) {
            e = ex;
        }

        assertNotNull(e);
    }

    @Test
    public void testMergeDiffEndpointOrdering() throws Exception {
        MockDefinition def = new MockDefinition.MockDefinitionBuilder("foo").endpointNotOrdered().build(null);

        IllegalArgumentException e = null;
        try {
            MockDefinition def1 = new MockDefinition.MockDefinitionBuilder("foo").build(def);
        } catch (IllegalArgumentException ex) {
            e = ex;
        }

        assertNotNull(e);
    }

    @Test
    public void testMergeDiffOrderingType() throws Exception {
        MockDefinition def = new MockDefinition.MockDefinitionBuilder("foo").ordering(MockDefinition.OrderingType.NONE).build(null);

        IllegalArgumentException e = null;
        try {
            MockDefinition def1 = new MockDefinition.MockDefinitionBuilder("foo").build(def);
        } catch (IllegalArgumentException ex) {
            e = ex;
        }

        assertNotNull(e);
    }

    @Test
    public void testMultipleMockEndpointFeeds() throws Exception {
        RouteDefinition rd = new RouteDefinition();
        rd.setGroup("foo");

        MockDefinition def = new MockDefinition.MockDefinitionBuilder("foo").addProcessors(new BodyProcessor(text("foo")),
                new BodyProcessor(text("baz"))).addPredicates(text("foo"), text("baz"))
                .mockFeederRoute(rd).build(null);

        IllegalArgumentException e = null;
        try {
            MockDefinition def1 = new MockDefinition.MockDefinitionBuilder("foo").mockFeederRoute(new RouteDefinition())
                    .build(def);
        } catch (IllegalArgumentException ex) {
            e = ex;
        }

        assertNotNull(e);
    }

    //we always take the first value
    @Test
    public void testMergeWaitTimes() throws Exception {
        MockDefinition def = new MockDefinition.MockDefinitionBuilder("foo").messageResultWaitTime(1234)
                .minimalResultWaitTime(5678).reassertionPeriod(314).expectedMessageCount(5).build(null);

        MockDefinition def1 = new MockDefinition.MockDefinitionBuilder("foo").messageResultWaitTime(1)
                        .expectedMessageCount(3).reassertionPeriod(413)
                        .minimalResultWaitTime(2).build(def);

        assertEquals(5678 + (8*1234),def1.getResultWaitTime());
        assertEquals(314,def1.getReassertionPeriod());
    }

    @Test
    public void testUnexpectedLenientProcessorFail() throws Exception {
        IllegalArgumentException e = null;

        try {
            MockDefinition def = new MockDefinition.MockDefinitionBuilder("").addProcessors(new BodyProcessor(text("foo")),
                    new BodyProcessor(text("baz"))).addPredicates(text("foo"), text("baz"))
                    .lenientProcessor(StubLenientProcessor.class).build(null);
        } catch (IllegalArgumentException ex) {
            e = ex;
        }

        assertNotNull(e);
    }

    @Test
    public void testMergeLenientSelectorFail() throws Exception {
        IllegalArgumentException e = null;

        MockDefinition def = new MockDefinition.MockDefinitionBuilder("foo").addProcessors(new BodyProcessor(text("foo")),
                new BodyProcessor(text("baz"))).addPredicates(text("foo"), text("baz")).lenient().build(null);

        try {
            MockDefinition def1 = new MockDefinition.MockDefinitionBuilder("foo").addProcessors(new BodyProcessor(text("foo")),
                    new BodyProcessor(text("baz"))).addPredicates(text("foo"), text("baz")).lenient().build(def);
        } catch (IllegalArgumentException ex) {
            e = ex;
        }

        assertNotNull(e);
    }

    @Test
    public void testReassertionPeriodWhenNoExpectedMessages() throws Exception {
        MockDefinition def = new MockDefinition.MockDefinitionBuilder("foo").messageResultWaitTime(1234)
                .minimalResultWaitTime(5678).reassertionPeriod(314).expectedMessageCount(0).build(null);

        assertEquals(5678,def.getReassertionPeriod());
    }

    @Test
    public void testMergeLenientSelector() throws Exception {
        MockDefinition def = new MockDefinition.MockDefinitionBuilder("foo").addProcessors(new BodyProcessor(text("foo")))
                .addProcessors(new BodyProcessor(text("baz"))).addPredicates(text("foo")).addPredicates(text("baz")).build(null);

        MockDefinition def1 = new MockDefinition.MockDefinitionBuilder("foo").addProcessors(new BodyProcessor(text("foo")))
                .addProcessors(new BodyProcessor(text("baz"))).addPredicates(text("foo")).addPredicates(text("baz")).lenient().build(def);

        assertEquals(2,def1.getExpectedMessageCount());
        assertEquals(2,def1.getProcessors().size());
        assertEquals(2,def1.getPredicates().size());
        assertNotNull(def1.getLenientProcessor());
        assertNotNull(def1.getLenientSelector());

    }

    public static class StubLenientProcessor extends MockDefinition.LenientProcessor {
        public StubLenientProcessor(List<Processor> processors) {
            super(processors);
        }
    }
}
