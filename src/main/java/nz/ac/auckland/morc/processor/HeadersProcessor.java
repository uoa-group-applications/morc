package nz.ac.auckland.morc.processor;

import nz.ac.auckland.morc.resource.TestResource;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * A processor for setting the headers of an exchange to the given set of values
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class HeadersProcessor implements Processor {

    private static final Logger logger = LoggerFactory.getLogger(HeadersProcessor.class);

    private TestResource<Map<String, Object>> responseHeaders;

    public HeadersProcessor(TestResource<Map<String, Object>> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        logger.trace("Setting headers of exchange from endpoint {} to {}", exchange.getFromEndpoint().getEndpointUri()
                , responseHeaders.getValue());
        exchange.getIn().setHeaders(responseHeaders.getValue());
    }
}