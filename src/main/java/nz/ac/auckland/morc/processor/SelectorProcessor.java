package nz.ac.auckland.morc.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Given a list of processors this class is used to select which should be used. The default implementation will cycle
 * through the responses/partProcessors. Any processors added should be thread safe.
 *
 * @author David MacDonald - d.macdonald@auckland.ac.nz
 */
public class SelectorProcessor implements Processor {
    private List<Processor> processors;
    private AtomicInteger messageIndex = new AtomicInteger(0);

    public SelectorProcessor(List<Processor> processors) {
        this.processors = processors;
    }

    protected List<Processor> getProcessors() {
        return Collections.unmodifiableList(processors);
    }

    public void process(Exchange exchange) throws Exception {
        if (processors.size() == 0) return;

        //the default implementation will cycle through the responses/partProcessors
        processors.get(messageIndex.getAndIncrement() % processors.size()).process(exchange);
    }
}