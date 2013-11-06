package nz.ac.auckland.integration.testing.validator;

import org.apache.camel.Exchange;
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
public class HttpExceptionValidator implements Validator {

    private static final Logger logger = LoggerFactory.getLogger(HttpExceptionValidator.class);

    private Validator responseBodyValidator;
    private Validator responseHeadersValidator;
    private int statusCode;

    public Validator getResponseBodyValidator() {
        return responseBodyValidator;
    }

    public Validator getResponseHeadersValidator() {
        return responseHeadersValidator;
    }

    public int getStatusCode() {
        return statusCode;
    }

    //keep the unwashed masses away
    private HttpExceptionValidator() {

    }

    @SuppressWarnings("unchecked")
    public boolean validate(Exchange e) {
        if (e == null) return false;
        Throwable t = e.getException();
        if (!(t instanceof HttpOperationFailedException)) return false;

        HttpOperationFailedException httpException = ((HttpOperationFailedException) t);

        String responseBody = httpException.getResponseBody();
        Map responseHeaders = httpException.getResponseHeaders();

        //this is a bit of a hack to use other validators
        Exchange validationExchange = new DefaultExchange(e);
        e.getIn().setBody(responseBody);
        e.getIn().setHeaders(responseHeaders);

        boolean validStatus = true, validBody = true, validHeaders = true;

        if (statusCode != 0 && statusCode != httpException.getStatusCode()) {
            logger.warn("HTTP Status Code is not expected, received: %s, expected: %s",httpException.getStatusCode(),
                    this.statusCode);
            validStatus = false;
        }

        if (responseBodyValidator != null && !responseBodyValidator.validate(validationExchange)) {
            logger.warn("The HTTP exception response body is not as expected");
            validBody = false;
        }

        if (responseHeadersValidator != null && !responseHeadersValidator.validate(validationExchange)) {
            logger.warn("The HTTP exception response headers are not as expected");
            validHeaders = false;
        }

        return validStatus && validBody && validHeaders;

    }

    public static class Builder {
        private Validator responseBodyValidator;
        private Validator responseHeadersValidator;
        private int statusCode;

        public Builder responseBodyValidator(Validator responseBodyValidator) {
            this.responseBodyValidator = responseBodyValidator;
            return this;
        }

        public Builder responseHeadersValidator(Validator responseHeadersValidator) {
            this.responseHeadersValidator = responseHeadersValidator;
            return this;
        }

        public Builder statusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public HttpExceptionValidator build() {
            HttpExceptionValidator validator = new HttpExceptionValidator();
            validator.responseBodyValidator = this.responseBodyValidator;
            validator.responseHeadersValidator = this.responseHeadersValidator;
            validator.statusCode = this.statusCode;
            return validator;
        }
    }
}
