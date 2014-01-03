package nz.ac.auckland.integration.testing.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ResponseHeadersProcessor implements Processor {

    private static final Logger logger = LoggerFactory.getLogger(ResponseHeadersProcessor.class);

    private List<Map<String,Object>> responseHeaders = new ArrayList<>();
    private AtomicInteger messageCount = new AtomicInteger();

    public void addResponseHeaders(Map<String,Object> responseBody) {
        responseHeaders.add(responseBody);
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        int responseIndex = messageCount.getAndIncrement();
        if (responseIndex > responseHeaders.size()) {
            logger.debug("...");
            if (responseHeaders.size() > 1)
                logger.warn("...");
            responseIndex = responseIndex % responseHeaders.size();
        }

        if (responseIndex <= responseHeaders.size())
            exchange.getOut().setHeaders(responseHeaders.get(responseIndex));
    }
}