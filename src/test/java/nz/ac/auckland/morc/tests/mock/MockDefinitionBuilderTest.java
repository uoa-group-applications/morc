package nz.ac.auckland.morc.tests.mock;

import nz.ac.auckland.morc.MorcMethods;
import nz.ac.auckland.morc.endpointoverride.EndpointOverride;
import nz.ac.auckland.morc.mock.MockDefinition;
import nz.ac.auckland.morc.processor.SelectorProcessor;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.model.RouteDefinition;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class MockDefinitionBuilderTest extends Assert implements MorcMethods {

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
        MockDefinition def = new MockDefinition.MockDefinitionBuilder("").addProcessors(text("foo"))
                .addProcessors(text("baz"))
                .addPredicates(text("foo"))
                .addPredicates(text("baz")).lenient().build(null);

        assertEquals(0, def.getPredicates().size());
        assertEquals(0, def.getProcessors().size());
        assertNotNull(def.getLenientSelector());
        assertEquals(def.getLenientProcessor().getClass(), SelectorProcessor.class);
    }

    @Test
    public void testCustomLenientSelectorWithExpectedMessages() throws Exception {
        Predicate predicate = new Predicate() {
            @Override
            public boolean matches(Exchange exchange) {
                return true;
            }
        };

        MockDefinition def = new MockDefinition.MockDefinitionBuilder("").addProcessors(text("foo"))
                .addProcessors(text("baz"))
                .addPredicates(text("foo"), text("baz")).lenient(predicate)
                .lenientProcessor(StubLenientProcessor.class).build(null);

        assertEquals(0, def.getPredicates().size());
        assertEquals(0, def.getProcessors().size());
        assertEquals(predicate, def.getLenientSelector());
        assertEquals(def.getLenientProcessor().getClass(), StubLenientProcessor.class);
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

    //we always take the first value
    @Test
    public void testMergeWaitTimes() throws Exception {
        MockDefinition def = new MockDefinition.MockDefinitionBuilder("foo").messageResultWaitTime(1234)
                .minimalResultWaitTime(5678).reassertionPeriod(314).expectedMessageCount(5).build(null);

        MockDefinition def1 = new MockDefinition.MockDefinitionBuilder("foo").messageResultWaitTime(1)

                .expectedMessageCount(3).reassertionPeriod(413)
                .minimalResultWaitTime(2).build(def);

        assertEquals(5678 + (8 * 1234), def1.getResultWaitTime());
        assertEquals(314, def1.getReassertionPeriod());
    }

    @Test
    public void testUnexpectedLenientProcessorFail() throws Exception {
        IllegalArgumentException e = null;

        try {
            MockDefinition def = new MockDefinition.MockDefinitionBuilder("").addProcessors(text("foo"), text("baz"))
                    .addPredicates(text("foo"), text("baz"))
                    .lenientProcessor(StubLenientProcessor.class).build(null);
        } catch (IllegalArgumentException ex) {
            e = ex;
        }

        assertNotNull(e);
    }

    @Test
    public void testMergeLenientSelectorFail() throws Exception {
        IllegalArgumentException e = null;

        MockDefinition def = new MockDefinition.MockDefinitionBuilder("foo").addProcessors(text("foo"), text("baz"))
                .addPredicates(text("foo"), text("baz")).lenient().build(null);

        try {
            MockDefinition def1 = new MockDefinition.MockDefinitionBuilder("foo").addProcessors(text("foo"), text("baz"))
                    .addPredicates(text("foo"), text("baz")).lenient().build(def);
        } catch (IllegalArgumentException ex) {
            e = ex;
        }

        assertNotNull(e);
    }

    @Test
    public void testReassertionPeriodWhenNoExpectedMessages() throws Exception {
        MockDefinition def = new MockDefinition.MockDefinitionBuilder("foo").messageResultWaitTime(1234)
                .minimalResultWaitTime(5678).reassertionPeriod(314).expectedMessageCount(0).build(null);

        assertEquals(5678, def.getReassertionPeriod());
    }

    @Test
    public void testMergeLenientSelector() throws Exception {
        MockDefinition def = new MockDefinition.MockDefinitionBuilder("foo").addProcessors(text("foo"))
                .addProcessors(text("baz")).addPredicates(text("foo")).addPredicates(text("baz")).build(null);

        MockDefinition def1 = new MockDefinition.MockDefinitionBuilder("foo").addProcessors(text("foo"))
                .addProcessors(text("baz")).addPredicates(text("foo")).addPredicates(text("baz")).lenient().build(def);

        assertEquals(2, def1.getExpectedMessageCount());
        assertEquals(2, def1.getProcessors().size());
        assertEquals(2, def1.getPredicates().size());
        assertNotNull(def1.getLenientProcessor());
        assertNotNull(def1.getLenientSelector());

    }

    @Test
    public void testMergeMockLenientMock() throws Exception {
        RouteDefinition rd = new RouteDefinition();
        rd.setGroup("foo");

        Processor pre = new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                exchange.setProperty("PreprocessorCalled", true);
            }
        };

        MockDefinition def = new MockDefinition.MockDefinitionBuilder("foo").addProcessors(text("foo"))
                .addProcessors(text("baz")).addPredicates(text("foo")).addPredicates(text("baz"))
                .endpointNotOrdered()
                .ordering(MockDefinition.OrderingType.NONE)
                .messageResultWaitTime(1234).minimalResultWaitTime(5678).reassertionPeriod(314)
                .addMockFeedPreprocessor(pre).build(null);

        MockDefinition def1 = new MockDefinition.MockDefinitionBuilder("foo").addProcessors(text("a"))
                .addProcessors(text("b")).addPredicates(text("a")).addPredicates(text("b")).lenient()
                .addEndpointOverride(new EndpointOverride() {
                    @Override
                    public void overrideEndpoint(Endpoint endpoint) {

                    }
                }).build(def);

        MockDefinition def2 = new MockDefinition.MockDefinitionBuilder("foo").addProcessors(text("moo"))
                .endpointNotOrdered().ordering(MockDefinition.OrderingType.NONE)
                .addProcessors(text("cow")).addPredicates(text("moo")).addPredicates(text("cow")).build(def1);

        Exchange e = new DefaultExchange(new DefaultCamelContext());

        assertEquals(4, def2.getExpectedMessageCount());
        assertEquals(4, def2.getPredicates().size());
        assertEquals(4, def2.getProcessors().size());
        assertEquals(5678 + (4 * 1234), def2.getResultWaitTime());
        assertEquals(314, def2.getReassertionPeriod());
        def2.getMockFeedPreprocessor().process(e);
        assertEquals("PreprocessorCalled", e.getProperty("PreprocessorCalled",Boolean.class));
        assertEquals(MockDefinition.OrderingType.NONE, def2.getOrderingType());
        assertFalse(def2.isEndpointOrdered());
        assertNotNull(def2.getLenientSelector());
        assertNotNull(def2.getLenientProcessor());

        e.getIn().setBody("foo");
        assertTrue(def2.getPredicates().get(0).matches(e));
        e.getIn().setBody("baz");
        assertTrue(def2.getPredicates().get(1).matches(e));
        e.getIn().setBody("moo");
        assertTrue(def2.getPredicates().get(2).matches(e));
        e.getIn().setBody("cow");
        assertTrue(def2.getPredicates().get(3).matches(e));

        def2.getProcessors().get(0).process(e);
        assertEquals("foo", e.getIn().getBody(String.class));
        def2.getProcessors().get(1).process(e);
        assertEquals("baz", e.getIn().getBody(String.class));
        def2.getProcessors().get(2).process(e);
        assertEquals("moo", e.getIn().getBody(String.class));
        def2.getProcessors().get(3).process(e);
        assertEquals("cow", e.getIn().getBody(String.class));

        assertTrue(def2.getLenientSelector().matches(e));

        //test it cycles through the responses
        def2.getLenientProcessor().process(e);
        assertEquals("a", e.getIn().getBody(String.class));
        def2.getLenientProcessor().process(e);
        assertEquals("b", e.getIn().getBody(String.class));
        def2.getLenientProcessor().process(e);
        assertEquals("a", e.getIn().getBody(String.class));
        def2.getLenientProcessor().process(e);
        assertEquals("b", e.getIn().getBody(String.class));
        def2.getLenientProcessor().process(e);
        assertEquals("a", e.getIn().getBody(String.class));

        assertEquals(3, def2.getEndpointOverrides().size());
    }

    @Test
    public void testMergeMockMockLenient() throws Exception {
        RouteDefinition rd = new RouteDefinition();
        rd.setGroup("foo");

        Processor pre = new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                exchange.setProperty("PreprocessorCalled", true);
            }
        };

        MockDefinition def = new MockDefinition.MockDefinitionBuilder("foo").addProcessors(text("foo"))
                .addProcessors(text("baz")).addPredicates(text("foo")).addPredicates(text("baz"))
                .endpointNotOrdered()
                .ordering(MockDefinition.OrderingType.NONE)
                .addMockFeedPreprocessor(pre).build(null);

        MockDefinition def1 = new MockDefinition.MockDefinitionBuilder("foo").addProcessors(text("moo"))
                .endpointNotOrdered().ordering(MockDefinition.OrderingType.NONE)
                        //these should be ignored
                .messageResultWaitTime(1234).minimalResultWaitTime(5678).reassertionPeriod(314)
                .addProcessors(text("cow")).addPredicates(text("moo")).addPredicates(text("cow")).build(def);

        MockDefinition def2 = new MockDefinition.MockDefinitionBuilder("foo").addProcessors(text("a"))
                .addProcessors(text("b")).addPredicates(text("a")).addPredicates(text("b")).lenient()
                .addEndpointOverride(new EndpointOverride() {
                    @Override
                    public void overrideEndpoint(Endpoint endpoint) {

                    }
                }).build(def1);

        Exchange e = new DefaultExchange(new DefaultCamelContext());

        assertEquals(4, def2.getExpectedMessageCount());
        assertEquals(4, def2.getPredicates().size());
        assertEquals(4, def2.getProcessors().size());
        assertEquals(10000 + (4 * 1000), def2.getResultWaitTime());
        assertEquals(0, def2.getReassertionPeriod());
        def2.getMockFeedPreprocessor().process(e);
        assertEquals("PreprocessorCalled", e.getProperty("PreprocessorCalled",Boolean.class));
        assertEquals(MockDefinition.OrderingType.NONE, def2.getOrderingType());
        assertFalse(def2.isEndpointOrdered());
        assertNotNull(def2.getLenientSelector());
        assertNotNull(def2.getLenientProcessor());

        e.getIn().setBody("foo");
        assertTrue(def2.getPredicates().get(0).matches(e));
        e.getIn().setBody("baz");
        assertTrue(def2.getPredicates().get(1).matches(e));
        e.getIn().setBody("moo");
        assertTrue(def2.getPredicates().get(2).matches(e));
        e.getIn().setBody("cow");
        assertTrue(def2.getPredicates().get(3).matches(e));

        def2.getProcessors().get(0).process(e);
        assertEquals("foo", e.getIn().getBody(String.class));
        def2.getProcessors().get(1).process(e);
        assertEquals("baz", e.getIn().getBody(String.class));
        def2.getProcessors().get(2).process(e);
        assertEquals("moo", e.getIn().getBody(String.class));
        def2.getProcessors().get(3).process(e);
        assertEquals("cow", e.getIn().getBody(String.class));

        assertTrue(def2.getLenientSelector().matches(e));

        //test it cycles through the responses
        def2.getLenientProcessor().process(e);
        assertEquals("a", e.getIn().getBody(String.class));
        def2.getLenientProcessor().process(e);
        assertEquals("b", e.getIn().getBody(String.class));
        def2.getLenientProcessor().process(e);
        assertEquals("a", e.getIn().getBody(String.class));
        def2.getLenientProcessor().process(e);
        assertEquals("b", e.getIn().getBody(String.class));
        def2.getLenientProcessor().process(e);
        assertEquals("a", e.getIn().getBody(String.class));

        assertEquals(3, def2.getEndpointOverrides().size());
    }

    @Test
    public void testEmptyProcessorsForLenientProcessor() throws Exception {
        MockDefinition def = new MockDefinition.MockDefinitionBuilder("foo").lenient().expectedMessageCount(5).build(null);

        assertNotNull(def.getLenientProcessor());
        assertNotNull(def.getLenientSelector());

        Exchange e = new DefaultExchange(new DefaultCamelContext());

        //ensure we can handle the case when no processors are available
        e.getIn().setBody("foo");
        def.getLenientProcessor().process(e);
        assertEquals("foo", e.getIn().getBody(String.class));
        assertEquals(0, def.getExpectedMessageCount());
    }

    @Test
    public void testLenientExcludesExpectedMessages() throws Exception {
        MockDefinition def = new MockDefinition.MockDefinitionBuilder("foo").lenient().build(null);

        assertNotNull(def.getLenientSelector());
        assertNotNull(def.getLenientProcessor());

        assertEquals(0, def.getExpectedMessageCount());

    }

    public static class StubLenientProcessor extends SelectorProcessor {
        public StubLenientProcessor(List<Processor> processors) {
            super(processors);
        }
    }
}
