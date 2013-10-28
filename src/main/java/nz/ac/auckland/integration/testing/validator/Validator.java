package nz.ac.auckland.integration.testing.validator;

import org.apache.camel.Exchange;

public interface Validator {

    public boolean validate(Exchange exchange);

}
