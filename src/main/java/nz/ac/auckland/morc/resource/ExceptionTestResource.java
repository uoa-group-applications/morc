package nz.ac.auckland.morc.resource;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A resource for testing that an exception is of the same type (or subclass)
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class ExceptionTestResource implements Processor, Predicate {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionTestResource.class);
    private Exception exception;

    public ExceptionTestResource() {
        this.exception = new Exception();
    }

    public ExceptionTestResource(Exception exception) {
        this.exception = exception;
    }

    @Override
    public boolean matches(Exchange exchange) {
        Throwable t = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);

        if (t == null) {
            logger.warn("An exception was expected to be received on endpoint {}",
                    (exchange.getFromEndpoint() != null ? exchange.getFromEndpoint().getEndpointUri() : "unknown"));
            return false;
        }

        logger.debug("An execution exception was encountered on endpoint {}",
                (exchange.getFromEndpoint() != null ? exchange.getFromEndpoint().getEndpointUri() : "unknown"), t);

        boolean matches = exception.getClass().isAssignableFrom(t.getClass());

        if (matches)
            logger.warn("The exception did not match the expected exception from endpoint {}",
                    (exchange.getFromEndpoint() != null ? exchange.getFromEndpoint().getEndpointUri() : "unknown"));

        return matches;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        exchange.setException(exception);
    }

    @Override
    public String toString() {
        return "ExceptionTestResource: " + exception;
    }
}
