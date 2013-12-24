package nz.ac.auckland.integration.testing.answer;

import org.apache.camel.Exchange;

/**
 * An interface to specify a particular response for an expectation
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public interface Answer<T> {
    /**
     * @param exchange  The exchange received by an expectation that will invoke a given response
     * @return          A response that will be given back to the client
     */
    public T response(Exchange exchange) throws Exception;
}
