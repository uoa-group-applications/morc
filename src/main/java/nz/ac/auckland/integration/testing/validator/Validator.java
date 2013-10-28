package nz.ac.auckland.integration.testing.validator;

import org.apache.camel.Exchange;

/**
 * An interface for validating responses or expectations
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public interface Validator {
    public boolean validate(Exchange exchange);
}
