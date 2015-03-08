package nz.ac.auckland.morc.resource;

import nz.ac.auckland.morc.predicate.HeadersPredicate;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.component.http.HttpOperationFailedException;
import org.apache.camel.impl.DefaultExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * A resource for returning non-200 error HTTP codes back to the client, or testing that such an response was received.
 * The default status code is 500.
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class HttpErrorTestResource<T extends Predicate & TestResource> extends HttpResponseTestResource {

    private static final Logger logger = LoggerFactory.getLogger(HttpErrorTestResource.class);

    public HttpErrorTestResource() {
        super(500);
    }

    public HttpErrorTestResource(int statusCode) {
        super(statusCode);
    }

    @SuppressWarnings("unchecked")
    public HttpErrorTestResource(int statusCode, T body) {
        super(statusCode, body);
    }

    @SuppressWarnings("unchecked")
    public HttpErrorTestResource(int statusCode, T body, HeadersTestResource headers) {
        super(statusCode, body, headers);
    }

    @Override
    public boolean matches(Exchange exchange) {
        if (exchange == null) return false;
        Throwable t = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        if (!(t instanceof HttpOperationFailedException)) {
            logger.error("An unexpected error occurred during exception validation", t);
            return false;
        }

        HttpOperationFailedException httpException = (HttpOperationFailedException) t;
        logger.debug("Validating exception {} on endpoint {}", httpException, (exchange.getFromEndpoint() != null ?
                exchange.getFromEndpoint().getEndpointUri() : "null"));

        String responseBody = httpException.getResponseBody();
        Map responseHeaders = httpException.getResponseHeaders();

        //this is a bit of a hack to use other validators
        Exchange validationExchange = new DefaultExchange(exchange);
        validationExchange.getIn().setBody(responseBody);
        validationExchange.getIn().setHeaders(responseHeaders);

        boolean validStatus = true, validBody = true, validHeaders = true;

        if (getStatusCode() != 0 && getStatusCode() != httpException.getStatusCode()) {
            logger.warn("HTTP Status Code is not expected, received: {}, expected: {}", httpException.getStatusCode(),
                    getStatusCode());
            validStatus = false;
        }

        if (getBody() != null && !getBody().matches(validationExchange)) {
            logger.warn("The HTTP exception response body is not as expected");
            validBody = false;
        }

        if (getHeaders() != null && !new HeadersPredicate(getHeaders()).matches(validationExchange)) {
            logger.warn("The HTTP exception response headers are not as expected");
            validHeaders = false;
        }

        return validStatus && validBody && validHeaders;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        super.process(exchange);
        exchange.getIn().setFault(true);
    }
}
