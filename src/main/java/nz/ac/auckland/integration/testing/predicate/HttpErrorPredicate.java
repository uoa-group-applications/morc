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

    private Predicate responseBodyValidator;
    private Predicate responseHeadersValidator;
    private int statusCode;

    public Predicate getResponseBodyValidator() {
        return responseBodyValidator;
    }

    public Predicate getResponseHeadersValidator() {
        return responseHeadersValidator;
    }

    public int getStatusCode() {
        return statusCode;
    }

    @SuppressWarnings("unchecked")
    public boolean matches(Exchange e) {
        if (e == null) return false;
        Throwable t = e.getException();
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

        if (responseBodyValidator != null && !responseBodyValidator.matches(validationExchange)) {
            logger.warn("The HTTP exception response body is not as expected");
            validBody = false;
        }

        if (responseHeadersValidator != null && !responseHeadersValidator.matches(validationExchange)) {
            logger.warn("The HTTP exception response headers are not as expected");
            validHeaders = false;
        }

        return validStatus && validBody && validHeaders;

    }

    public static class Builder {
        private Predicate responseBodyValidator;
        private Predicate responseHeadersValidator;
        private int statusCode;

        /**
         * @param responseBodyValidator A validator for the expected error response body
         */
        public Builder responseBodyValidator(Predicate responseBodyValidator) {
            this.responseBodyValidator = responseBodyValidator;
            return this;
        }

        /**
         * @param responseHeadersPredicate A validator for the HTTP response headers
         */
        public Builder responseHeadersValidator(HeadersPredicate responseHeadersPredicate) {
            this.responseHeadersValidator = responseHeadersPredicate;
            return this;
        }

        /**
         * @param resource A resource containing the expected response headers
         */
        @SuppressWarnings("unchecked")
        public Builder responseHeadersValidator(HeadersTestResource resource) {
            this.responseHeadersValidator = new HeadersPredicate(resource);
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
            predicate.responseBodyValidator = this.responseBodyValidator;
            predicate.responseHeadersValidator = this.responseHeadersValidator;
            predicate.statusCode = this.statusCode;
            return predicate;
        }
    }
}
