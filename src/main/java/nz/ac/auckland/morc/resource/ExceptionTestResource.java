package nz.ac.auckland.morc.resource;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A resource for using or validating Java exceptions where validation only checks that the message is the same,
 * and of the same type (or sub-type)
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
            logger.warn("An exception was expected to be received on endpoint {}", exchange.getFromEndpoint().getEndpointUri());
            return false;
        }

        logger.debug("An execution exception was encountered on endpoint {}", exchange.getFromEndpoint().getEndpointUri(), t);

        boolean matches = (t.getMessage().equals(exception.getMessage())) && exception.getClass().isAssignableFrom(t.getClass());

        if (!matches)
            logger.warn("The exception did not match the expected exception from endpoint {}",
                    exchange.getFromEndpoint().getEndpointUri());

        return matches;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        exchange.setException(exception);
    }
}
