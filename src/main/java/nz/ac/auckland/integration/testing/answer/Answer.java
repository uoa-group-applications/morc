package nz.ac.auckland.integration.testing.answer;

import org.apache.camel.Exchange;

public interface Answer<T> {
    public T response(Exchange exchange) throws Exception;
}
