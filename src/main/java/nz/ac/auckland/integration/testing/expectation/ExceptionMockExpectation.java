package nz.ac.auckland.integration.testing.expectation;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;

/**
 * This endpoint will throw an exception of the specified type back to the
 * message consumer; this is likely to cause transaction rollback or some kind
 * of SOAP fault
 * <p/>
 * If no exception is specified then an empty Exception is thrown
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class ExceptionMockExpectation extends MockExpectation {
    private static final Logger logger = LoggerFactory.getLogger(ExceptionMockExpectation.class);

    private Class<? extends Exception> exceptionClass;
    private String exceptionMessage;

    public Class getExceptionClass() {
        return exceptionClass;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    /**
     * This will throw the specified exception back to Camel for handling as appropriate
     */
    @SuppressWarnings("unchecked")
    public void handleReceivedExchange(Exchange exchange) throws Exception {

        Exception e;

        if (exceptionClass == null) {
            if (exceptionMessage != null) e = new Exception(exceptionMessage);
            else e = new Exception();
        } else if (exceptionMessage != null) {
            Constructor<? extends Exception> constructor = exceptionClass.getConstructor(String.class);
            e = constructor.newInstance(exceptionMessage);
        } else {
            Constructor<? extends Exception> constructor = exceptionClass.getConstructor();
            e = constructor.newInstance();
        }

        logger.debug("Throwing the exception {}", e);
        throw e;
    }

    public String getType() {
        return "exception";
    }

    public static class Builder extends MockExpectation.AbstractBuilder<ExceptionMockExpectation, Builder> {

        private Class<? extends Exception> exceptionClass;
        private String exceptionMessage;

        public Builder(String endpointUri) {
            super(endpointUri);
        }

        protected Builder self() {
            return this;
        }

        /**
         * @param exceptionClass The exception that should be instantiated
         */
        public Builder exceptionClass(Class<? extends Exception> exceptionClass) {
            this.exceptionClass = exceptionClass;
            return self();
        }

        /**
         * @param exceptionMessage The message that should be added to the exception on instantiation
         */
        public Builder message(String exceptionMessage) {
            this.exceptionMessage = exceptionMessage;
            return self();
        }

        /**
         * @throws IllegalArgumentException If no suitable constructor is available for an exception when a message is (or isn't) specified
         */
        public ExceptionMockExpectation build() {
            //check we have a message constructor
            if (exceptionClass != null && exceptionMessage != null) {
                try {
                    exceptionClass.getConstructor(String.class);
                } catch (NoSuchMethodException e) {
                    throw new IllegalArgumentException(e);
                }
            }

            //check we have a default constructor
            if (exceptionMessage == null && exceptionClass != null) {
                try {
                    exceptionClass.getConstructor();
                } catch (NoSuchMethodException e) {
                    throw new IllegalArgumentException(e);
                }
            }

            return new ExceptionMockExpectation(this);
        }
    }

    protected ExceptionMockExpectation(Builder builder) {
        super(builder);

        this.exceptionClass = builder.exceptionClass;
        this.exceptionMessage = builder.exceptionMessage;
    }
}
