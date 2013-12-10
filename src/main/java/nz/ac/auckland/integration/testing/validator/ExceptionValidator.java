package nz.ac.auckland.integration.testing.validator;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExceptionValidator implements Validator {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionValidator.class);
    private Class<? extends Exception> expectedExceptionClass;
    private String message;

    public ExceptionValidator() {
        this.expectedExceptionClass = Exception.class;
    }


    public ExceptionValidator(Class<? extends Exception> expectedExceptionClass) {
        this.expectedExceptionClass = expectedExceptionClass;
    }

    public ExceptionValidator(Class<? extends Exception> expectedExceptionClass, String message) {
        this(expectedExceptionClass);
        this.message = message;
    }

    public boolean validate(Exchange exchange) {
        Exception e = exchange.getException();

        if (e == null) {
            logger.warn("An exception was expected to be received");
            return false;
        }

        logger.debug("An execution exception was encountered", e);
        return e.getClass().equals(expectedExceptionClass) || message == null || message.equals(e.getMessage());
    }
}
