package nz.ac.auckland.integration.testing.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.util.List;

/**
 * A class for aggregating multiple processors to appear as one such that it's easier to use outside of the builder
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class MultiProcessor implements Processor {

    protected List<Processor> processors;

    public MultiProcessor(List<Processor> processors) {
        this.processors = processors;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        //todo: add logging
        for (Processor processor : processors) {
            processor.process(exchange);
        }
    }
}