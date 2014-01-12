package nz.ac.auckland.integration.testing.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResponseBodyProcessor implements Processor {

    private static final Logger logger = LoggerFactory.getLogger(ResponseBodyProcessor.class);

    private Object responseBody;

    public ResponseBodyProcessor(Object responseBody) {
        this.responseBody = responseBody;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        exchange.getOut().setBody(responseBody);
    }
}