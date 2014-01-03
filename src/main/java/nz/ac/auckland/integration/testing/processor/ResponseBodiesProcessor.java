package nz.ac.auckland.integration.testing.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ResponseBodiesProcessor implements Processor {

    private static final Logger logger = LoggerFactory.getLogger(ResponseBodiesProcessor.class);

    private List responseBodies = new ArrayList();
    private AtomicInteger messageCount = new AtomicInteger();

    public void addResponseBody(Object responseBody) {
        responseBodies.add(responseBody);
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        int responseIndex = messageCount.getAndIncrement();
        if (responseIndex > responseBodies.size()) {
            logger.debug("...");
            if (responseBodies.size() > 1)
                logger.warn("...");
            responseIndex = responseIndex % responseBodies.size();
        }

        if (responseBodies.size() == 0) {
            logger.warn("No response bodies were provided and an empty response will be provided");
            exchange.getOut().setBody("");
        } else exchange.getOut().setBody(responseBodies.get(responseIndex));
    }
}