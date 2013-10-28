package nz.ac.auckland.integration.testing.validator;

import nz.ac.auckland.integration.testing.resource.StaticTestResource;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;

public class SOAPFaultValidator implements Validator {

    public boolean validate(Exchange e) {

        Throwable t = e.getCause();
        return false;
    }
}
