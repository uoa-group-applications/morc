package nz.ac.auckland.integration.testing.validators;

import nz.ac.auckland.integration.testing.resource.TestResource;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.component.http.HttpOperationFailedException;

/**
 * Determines whether the exception contains a message matching the expected response for an HTTP 500 status
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class HttpExceptionResponseValidator implements ExceptionValidator {
    @Override
    public boolean validateInput(CamelExecutionException e, TestResource<String> expectedResponseBody) {
        Throwable t = e.getCause();
        if (!(t instanceof HttpOperationFailedException)) return false;
        String responseBody = ((HttpOperationFailedException) t).getResponseBody();
        return expectedResponseBody == null || expectedResponseBody.validateInput(responseBody);
    }
}
