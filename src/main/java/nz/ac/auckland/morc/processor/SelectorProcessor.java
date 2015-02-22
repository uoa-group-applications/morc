package nz.ac.auckland.morc.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

//any processors added should be thread safe!
public class SelectorProcessor implements Processor {
    private List<Processor> processors;
    private AtomicInteger messageIndex = new AtomicInteger(0);

    public SelectorProcessor(List<Processor> processors) {
        this.processors = processors;
    }

    public void process(Exchange exchange) throws Exception {
        if (processors.size() == 0) return;

        //the default implementation will cycle through the responses/partProcessors
        processors.get(messageIndex.getAndIncrement() % processors.size()).process(exchange);
    }
}