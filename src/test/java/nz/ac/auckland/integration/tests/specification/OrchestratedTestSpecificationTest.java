package nz.ac.auckland.integration.tests.specification;

import nz.ac.auckland.integration.testing.expectation.ExceptionMockExpectation;
import nz.ac.auckland.integration.testing.specification.OrchestratedTestSpecification;
import org.apache.camel.ProducerTemplate;
import org.junit.Assert;
import org.junit.Test;

public class OrchestratedTestSpecificationTest extends Assert {

    @Test
    public void testSetValues() throws Exception {
        TestOrchestratedTestSpecification spec = new TestOrchestratedTestSpecification
                .Builder("targetUri", "description")
                .addExpectation(new ExceptionMockExpectation.Builder("targetUri"))
                .build();

        assertEquals("description", spec.getDescription());
        assertEquals("targetUri", spec.getTargetServiceUri());
        assertEquals(1, spec.getMockExpectations().size());
    }

    @Test
    public void testExpectationOrdering() throws Exception {
        try {
            TestOrchestratedTestSpecification spec = new TestOrchestratedTestSpecification
                    .Builder("targetUri", "description")
                    .addExpectation(new ExceptionMockExpectation.Builder("targetUri").endpointNotOrdered())
                    .addExpectation(new ExceptionMockExpectation.Builder("targetUri"))
                    .build();

            Assert.fail("Should have been an error about endpoint ordered state");
        } catch (IllegalArgumentException e) {

        }
    }

    @Test
    public void testDifferentOrderingRequirementsException() throws Exception {
        TestOrchestratedTestSpecification spec = new TestOrchestratedTestSpecification
                .Builder("targetUri", "description")
                .addExpectation(new ExceptionMockExpectation.Builder("targetUri").expectedMessageCount(2))
                .addExpectation(new ExceptionMockExpectation.Builder("targetUri1"))
                .build();

        assertEquals(0, spec.getMockExpectations().get(0).getReceivedAt());
        assertEquals(2, spec.getMockExpectations().get(1).getReceivedAt());

    }
}

class TestOrchestratedTestSpecification extends OrchestratedTestSpecification {

    @Override
    public boolean sendInput(ProducerTemplate template) {
        return true;
    }

    public static class Builder extends TestOrchestratedTestSpecification.AbstractBuilder<TestOrchestratedTestSpecification, Builder> {

        public Builder(String endpointUri, String description) {
            super(endpointUri, description);
        }

        protected Builder self() {
            return this;
        }


        public TestOrchestratedTestSpecification build() {
            return new TestOrchestratedTestSpecification(this);
        }
    }

    protected TestOrchestratedTestSpecification(Builder builder) {
        super(builder);

    }
}