package nz.ac.auckland.integration.testing.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * A processor for setting the headers of an exchange to the given set of values
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class HeadersProcessor implements Processor {

    private static final Logger logger = LoggerFactory.getLogger(HeadersProcessor.class);

    private Map<String, Object> responseHeaders = new HashMap<>();

    public HeadersProcessor(Map<String, Object> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        logger.trace("Setting headers of exchange from endpoint {} to {}", exchange.getFromEndpoint().getEndpointUri()
                , responseHeaders);
        exchange.getIn().setHeaders(responseHeaders);
    }
}