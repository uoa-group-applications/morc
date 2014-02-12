package nz.ac.auckland.integration.tests.mock;

import nz.ac.auckland.integration.testing.mock.MockDefinition;
import nz.ac.auckland.integration.testing.mock.builder.AsyncMockDefinitionBuilder;
import org.junit.Assert;
import org.junit.Test;

public class AsyncMockDefinitionBuilderTest extends Assert {

    @Test
    public void testReconfiguredToPartialOrdering() throws Exception {
        MockDefinition def = new AsyncMockDefinitionBuilder("foo").ordering(MockDefinition.OrderingType.TOTAL).build(null);
        assertEquals(MockDefinition.OrderingType.PARTIAL, def.getOrderingType());
    }

}
