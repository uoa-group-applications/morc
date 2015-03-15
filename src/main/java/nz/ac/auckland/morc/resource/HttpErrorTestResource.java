package nz.ac.auckland.morc.resource;

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
 * Setting the status code to 0 will mean it won't be validated against (but the body and headers will, if any).
 *
 * @author David MacDonald - d.macdonald@auckland.ac.nz
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

    @SuppressWarnings("unchecked")
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
        validationExchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, httpException.getStatusCode());

        return super.matches(validationExchange);
    }

    @Override
    public String toString() {
        return "HttpErrorTestResource: Code:" + getStatusCode() + ", Body: " +
                (getBody() != null ? getBody().toString() : ", Body: null");
    }

}
