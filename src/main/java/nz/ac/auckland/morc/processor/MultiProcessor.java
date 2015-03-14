package nz.ac.auckland.morc.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * A class for aggregating multiple processors to appear as one such that it's easier to use outside of the builder
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class MultiProcessor implements Processor {

    protected List<Processor> processors;
    private static final Logger logger = LoggerFactory.getLogger(MultiProcessor.class);

    public MultiProcessor(List<Processor> processors) {
        this.processors = processors;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        logger.trace("Applying {} processors against exchange from endpoint {}", processors.size(),
                (exchange.getFromEndpoint() != null ? exchange.getFromEndpoint().getEndpointUri() : "unknown"));
        for (Processor processor : processors) {
            logger.trace("Applying processor");
            processor.process(exchange);
        }
    }
}