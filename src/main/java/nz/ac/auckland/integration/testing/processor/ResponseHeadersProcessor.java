package nz.ac.auckland.integration.testing.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ResponseHeadersProcessor implements Processor {

    private static final Logger logger = LoggerFactory.getLogger(ResponseHeadersProcessor.class);

    private Map<String,Object> responseHeaders = new HashMap<>();

    public ResponseHeadersProcessor(Map<String,Object> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        exchange.getOut().setHeaders(responseHeaders);
    }
}