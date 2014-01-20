package nz.ac.auckland.integration.testing.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.util.List;

/**
 * User: d.macdonald@auckland.ac.nz
 * Date: 20/01/14
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