package nz.ac.auckland.morc.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.http.HttpOperationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class HttpExceptionResponseProcessor implements Processor {
    private static final Logger logger = LoggerFactory.getLogger(HttpExceptionResponseProcessor.class);

    @SuppressWarnings("unchecked")
    @Override
    public void process(Exchange exchange) {
        Throwable t = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        if (!(t instanceof HttpOperationFailedException)) return;

        HttpOperationFailedException httpException = (HttpOperationFailedException) t;

        String responseBody = httpException.getResponseBody();
        Map<String, String> responseHeaders = httpException.getResponseHeaders();

        exchange.getIn().setBody(responseBody);
        exchange.getIn().getHeaders().putAll(responseHeaders);
        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE,httpException.getStatusCode());

        StringBuilder builder = new StringBuilder();
        for (String key : responseHeaders.keySet()) {
            builder.append(key).append(":").append(responseHeaders.get(key)).append(",");
        }

        logger.debug("Received error response from endpoint: {}, body: {}, headers: {}",
                new String[]{(exchange.getFromEndpoint() != null ? exchange.getFromEndpoint().getEndpointUri() :
                        "unknown"), responseBody, builder.toString()});
    }
}
