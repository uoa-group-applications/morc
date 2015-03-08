package nz.ac.auckland.morc.resource;

import nz.ac.auckland.morc.predicate.HeadersPredicate;
import nz.ac.auckland.morc.processor.BodyProcessor;
import nz.ac.auckland.morc.processor.HeadersProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A resource for returning non-200 HTTP codes back to the client, or testing that such an response was received
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class HttpResponseTestResource<T extends Predicate & TestResource> implements Processor, Predicate {

    private static final Logger logger = LoggerFactory.getLogger(HttpResponseTestResource.class);
    private int statusCode;
    private T body;
    private HeadersTestResource headers;

    public HttpResponseTestResource() {
        this.statusCode = 200;
    }

    public HttpResponseTestResource(int statusCode) {
        this.statusCode = statusCode;
    }

    public HttpResponseTestResource(int statusCode, T body) {
        this.statusCode = statusCode;
        this.body = body;
    }

    public HttpResponseTestResource(int statusCode, T body, HeadersTestResource headers) {
        this.statusCode = statusCode;
        this.body = body;
        this.headers = headers;
    }

    protected int getStatusCode() {
        return this.statusCode;
    }

    protected T getBody() {
        return body;
    }

    protected HeadersTestResource getHeaders() {
        return headers;
    }

    @Override
    public boolean matches(Exchange exchange) {
        if (exchange == null) return false;

        boolean validStatus = true, validBody = true, validHeaders = true;

        int receivedStatusCode = exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
        if (statusCode != 0 && statusCode != receivedStatusCode) {
            logger.warn("HTTP Status Code is not expected, received: {}, expected: {}", receivedStatusCode,
                    this.statusCode);
            validStatus = false;
        }

        if (body != null && !body.matches(exchange)) {
            logger.warn("The HTTP response body is not as expected");
            validBody = false;
        }

        if (headers != null && !new HeadersPredicate(headers).matches(exchange)) {
            logger.warn("The HTTP response headers are not as expected");
            validHeaders = false;
        }

        return validStatus && validBody && validHeaders;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        logger.trace("Setting response code to {} for endpoint {}", statusCode, exchange.getFromEndpoint().getEndpointUri());
        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, statusCode);
        if (body != null) new BodyProcessor(body).process(exchange);
        if (headers != null) new HeadersProcessor(headers).process(exchange);
    }
}
