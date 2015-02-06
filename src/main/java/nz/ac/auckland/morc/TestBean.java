package nz.ac.auckland.morc;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.junit.Assert;

public abstract class TestBean extends Assert implements Processor {
    public abstract void run() throws Exception;

    @Override
    public void process(Exchange exchange) throws Exception {
        run();
    }
}
