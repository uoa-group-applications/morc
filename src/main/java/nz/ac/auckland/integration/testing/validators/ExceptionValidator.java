package nz.ac.auckland.integration.testing.validators;

import nz.ac.auckland.integration.testing.resource.TestResource;
import org.apache.camel.CamelExecutionException;

/**
 * Because an exception may be different for a given endpoint we need a general way to validate it
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public abstract interface ExceptionValidator {
    /**
     * @param e                    The exception containing the error response message
     * @param expectedResponseBody The body of the expected (error) response
     * @return true if the error response message in the exception matches the expected response body
     */
    public boolean validateInput(CamelExecutionException e, TestResource<String> expectedResponseBody);
}
