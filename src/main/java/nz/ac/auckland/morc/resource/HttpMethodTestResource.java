package nz.ac.auckland.morc.resource;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A resource for returning or validating particular HTTP methods
 *
 * @author David MacDonald - d.macdonald@auckland.ac.nz
 */
public class HttpMethodTestResource<T extends Processor & Predicate> implements Processor, Predicate {

    private static final Logger logger = LoggerFactory.getLogger(HttpMethodTestResource.class);
    private HttpMethod httpMethod;

    public HttpMethodTestResource(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
    }

    @Override
    public boolean matches(Exchange exchange) {
        if (exchange == null) return false;

        if (exchange.getIn().getHeader(Exchange.HTTP_METHOD) == null) {
            logger.warn("The HTTP method does not exist");
            return false;
        }

        String receivedMethod = exchange.getIn().getHeader(Exchange.HTTP_METHOD, String.class);
        if (!httpMethod.name().equals(receivedMethod)) {
            logger.warn("HTTP Method is not expected, received {}, expected: {}", receivedMethod, httpMethod.name());
            return false;
        }

        return true;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        logger.trace("Setting response method to {} for endpoint {}", httpMethod.name(),
                (exchange.getFromEndpoint() != null ? exchange.getFromEndpoint().getEndpointUri() : "unknown"));
        exchange.getIn().setHeader(Exchange.HTTP_METHOD, httpMethod.name());
    }

    public enum HttpMethod {
        GET, POST, PUT, DELETE, HEAD, OPTIONS, TRACE
    }

    @Override
    public String toString() {
        return "HttpMethodTestResource: Method:" + httpMethod.name();
    }
}
