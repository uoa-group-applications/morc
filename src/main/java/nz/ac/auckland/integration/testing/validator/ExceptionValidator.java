package nz.ac.auckland.integration.testing.validator;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class for ensuring an exception received is as expected based on the class and message
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class ExceptionValidator implements Validator {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionValidator.class);
    private Class<? extends Exception> expectedExceptionClass;
    private String message;

    /**
     * A constructor that simply expects a subclass of java.lang.Exception
     */
    public ExceptionValidator() {
        this.expectedExceptionClass = Exception.class;
    }

    /**
     * @param expectedExceptionClass The class of exception we expect to validate against
     */
    public ExceptionValidator(Class<? extends Exception> expectedExceptionClass) {
        this.expectedExceptionClass = expectedExceptionClass;
    }

    /**
     * @param expectedExceptionClass    The class of exception we expect to validate against
     * @param message                   The message this instance should be returning
     */
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
