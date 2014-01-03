package nz.ac.auckland.integration.testing.validator;

import org.apache.camel.Exchange;

public class ConstantValidator<T> implements Validator {

    private boolean response;

    public ConstantValidator() {
        response = true;
    }

    public ConstantValidator(boolean response) {
        this.response = response;
    }

    @Override
    public boolean validate(Exchange exchange) {
        return response;
    }
}
