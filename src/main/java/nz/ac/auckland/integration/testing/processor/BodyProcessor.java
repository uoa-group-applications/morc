package nz.ac.auckland.integration.testing.processor;

import nz.ac.auckland.integration.testing.resource.TestResource;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A processor for setting the body of an exchange to a given value
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class BodyProcessor implements Processor {

    private static final Logger logger = LoggerFactory.getLogger(BodyProcessor.class);

    private Object responseBody;

    public BodyProcessor(Object responseBody) {
        this.responseBody = responseBody;
    }

    public BodyProcessor(TestResource responseTestResource) {
        try {
            this.responseBody = responseTestResource.getValue();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        logger.trace("Setting body of exchange from endpoint {} to {}", exchange.getFromEndpoint().getEndpointUri(), responseBody);
        exchange.getIn().setBody(responseBody);
    }
}