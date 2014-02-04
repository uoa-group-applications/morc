package nz.ac.auckland.integration.testing.predicate;

import nz.ac.auckland.integration.testing.resource.HeadersTestResource;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.component.http.HttpOperationFailedException;
import org.apache.camel.impl.DefaultExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * For validating the response exception is as expected
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class HttpErrorPredicate implements Predicate {

    private static final Logger logger = LoggerFactory.getLogger(HttpErrorPredicate.class);

    private Predicate bodyValidator;
    private Predicate headersValidator;
    private int statusCode;

    public Predicate getBodyValidator() {
        return bodyValidator;
    }

    public Predicate getHeadersValidator() {
        return headersValidator;
    }

    public int getStatusCode() {
        return statusCode;
    }

    @SuppressWarnings("unchecked")
    public boolean matches(Exchange e) {
        if (e == null) return false;
        Throwable t = e.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        if (!(t instanceof HttpOperationFailedException)) {
            logger.error("An unexpected error occurred during exception validation", t);
            return false;
        }

        HttpOperationFailedException httpException = (HttpOperationFailedException) t;

        String responseBody = httpException.getResponseBody();
        Map responseHeaders = httpException.getResponseHeaders();

        //this is a bit of a hack to use other validators
        Exchange validationExchange = new DefaultExchange(e);
        validationExchange.getIn().setBody(responseBody);
        validationExchange.getIn().setHeaders(responseHeaders);

        boolean validStatus = true, validBody = true, validHeaders = true;

        if (statusCode != 0 && statusCode != httpException.getStatusCode()) {
            logger.warn("HTTP Status Code is not expected, received: {}, expected: {}", httpException.getStatusCode(),
                    this.statusCode);
            validStatus = false;
        }

        if (bodyValidator != null && !bodyValidator.matches(validationExchange)) {
            logger.warn("The HTTP exception response body is not as expected");
            validBody = false;
        }

        if (headersValidator != null && !headersValidator.matches(validationExchange)) {
            logger.warn("The HTTP exception response headers are not as expected");
            validHeaders = false;
        }

        return validStatus && validBody && validHeaders;

    }

    public static class Builder {
        private Predicate bodyValidator;
        private Predicate headersValidator;
        private int statusCode;

        /**
         * @param bodyValidator A validator for the expected error response body
         */
        public Builder bodyValidator(Predicate bodyValidator) {
            this.bodyValidator = bodyValidator;
            return this;
        }

        /**
         * @param headersPredicate A validator for the HTTP response headers
         */
        public Builder headersValidator(HeadersPredicate headersPredicate) {
            this.headersValidator = headersPredicate;
            return this;
        }

        /**
         * @param resource A resource containing the expected response headers
         */
        @SuppressWarnings("unchecked")
        public Builder headersValidator(HeadersTestResource resource) {
            this.headersValidator = new HeadersPredicate(resource);
            return this;
        }

        /**
         * @param statusCode The expected response status code
         */
        public Builder statusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public HttpErrorPredicate build() {
            HttpErrorPredicate predicate = new HttpErrorPredicate();
            predicate.bodyValidator = this.bodyValidator;
            predicate.headersValidator = this.headersValidator;
            predicate.statusCode = this.statusCode;
            return predicate;
        }
    }
}
