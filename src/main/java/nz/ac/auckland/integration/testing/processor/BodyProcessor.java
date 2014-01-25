package nz.ac.auckland.integration.testing.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BodyProcessor implements Processor {

    private static final Logger logger = LoggerFactory.getLogger(BodyProcessor.class);

    private Object responseBody;

    public BodyProcessor(Object responseBody) {
        this.responseBody = responseBody;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        exchange.getOut().setBody(responseBody);
    }
}