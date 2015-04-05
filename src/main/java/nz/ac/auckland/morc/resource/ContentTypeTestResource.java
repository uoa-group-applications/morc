package nz.ac.auckland.morc.resource;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class to set the Content-Type header in a response, or validate it was returned correctly
 *
 * @author David MacDonald - d.macdonald@auckland.ac.nz
 */
public class ContentTypeTestResource implements Processor, Predicate {

    private static final Logger logger = LoggerFactory.getLogger(ContentTypeTestResource.class);
    private String contentType;

    public ContentTypeTestResource() {
        contentType = "text/plain";
    }

    public ContentTypeTestResource(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public boolean matches(Exchange exchange) {
        String contentType = exchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class);

        logger.debug("Expected Content-Type Header: {}, Actual Content-Type Header: {}", this.contentType,
                contentType);

        boolean result = contentType != null && contentType.equals(this.contentType);
        if (!result) logger.warn("Content-Type headers did not match, received: {}", contentType);

        return result;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        logger.debug("Setting Content-Type header to {}", contentType);
        exchange.getIn().setHeader(Exchange.CONTENT_TYPE, contentType);
    }
}
