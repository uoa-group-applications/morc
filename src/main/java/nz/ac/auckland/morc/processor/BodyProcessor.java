package nz.ac.auckland.morc.processor;

import nz.ac.auckland.morc.resource.TestResource;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A processor for setting the body of an exchange to a given value
 *
 * @author David MacDonald - d.macdonald@auckland.ac.nz
 */
public class BodyProcessor implements Processor {

    private static final Logger logger = LoggerFactory.getLogger(BodyProcessor.class);

    private TestResource responseBody;

    public BodyProcessor(TestResource responseTestResource) {
        this.responseBody = responseTestResource;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        logger.trace("Setting body of exchange from endpoint {} to {}",
                (exchange.getFromEndpoint() != null ? exchange.getFromEndpoint().getEndpointUri() : "unknown"),
                responseBody.getValue());
        exchange.getIn().setBody(responseBody.getValue());
    }
}