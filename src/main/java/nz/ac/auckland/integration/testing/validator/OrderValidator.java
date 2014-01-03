package nz.ac.auckland.integration.testing.validator;

import nz.ac.auckland.integration.testing.mock.MockExpectation;
import org.apache.camel.Exchange;

public class OrderValidator implements Validator {

    private int expectedIndex;
    private MockExpectation.OrderingType orderingType;

    public OrderValidator(int expectedIndex, MockExpectation.OrderingType orderingType) {
        this.expectedIndex = expectedIndex;
        this.orderingType = orderingType;
    }

    @Override
    public boolean validate(Exchange exchange, int index) {

        if (orderingType == MockExpectation.OrderingType.NONE) return true;

        if (orderingType == MockExpectation.OrderingType.TOTAL && index == expectedIndex) return true;

        //partially ordered exchanges can occur in the future
        return (orderingType == MockExpectation.OrderingType.PARTIAL && index >= expectedIndex);
    }
}
