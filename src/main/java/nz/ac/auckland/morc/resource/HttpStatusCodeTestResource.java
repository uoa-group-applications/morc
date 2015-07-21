package nz.ac.auckland.morc.resource;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.component.http.HttpOperationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A resource for returning non-200 HTTP codes back to the client, or testing that such an response was received
 *
 * @author David MacDonald - d.macdonald@auckland.ac.nz
 */
public class HttpStatusCodeTestResource<T extends Processor & Predicate> implements Processor, Predicate {

    private static final Logger logger = LoggerFactory.getLogger(HttpStatusCodeTestResource.class);
    private int statusCode;

    public HttpStatusCodeTestResource(int statusCode) {
        this.statusCode = statusCode;
    }

    @Override
    public boolean matches(Exchange exchange) {
        if (exchange == null) return false;

        if (exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE) == null) {
            logger.warn("The HTTP response code does not exist");
            return false;
        }

        int receivedStatusCode = exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
        if (statusCode != 0 && statusCode != receivedStatusCode) {
            logger.warn("HTTP Status Code is not expected, received: {}, expected: {}", receivedStatusCode,
                    this.statusCode);
            return false;
        }

        if (statusCode >= 400) {
            Throwable t = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
            if (!(t instanceof HttpOperationFailedException)) {
                logger.error("An HttpOperationFailedException response is expected for status code {} but received {}",
                        statusCode, t);
                //noting that there is other code that yoinks out the responseBody/responseHeaders and validates
                //against these
                return false;
            }
            HttpOperationFailedException httpException = (HttpOperationFailedException) t;
            logger.debug("Validated exception {} on endpoint {}", httpException, (exchange.getFromEndpoint() != null ?
                    exchange.getFromEndpoint().getEndpointUri() : "null"));
        }

        return true;
    }

    public void process(Exchange exchange) throws Exception {
        logger.trace("Setting response code to {} for endpoint {}", statusCode,
                (exchange.getFromEndpoint() != null ? exchange.getFromEndpoint().getEndpointUri() : "unknown"));
        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, statusCode);
    }

    @Override
    public String toString() {
        return "HttpStatusCodeTestResource: Status:" + statusCode;
    }
}