package nz.ac.auckland.morc;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.junit.Assert;

/**
 * Rather than sending to a Camel endpoint it is possible to run arbitrary Java code for
 * special cases
 *
 * @author David MacDonald - d.macdonald@auckland.ac.nz
 */
public abstract class TestBean extends Assert implements Processor {
    public abstract void run() throws Exception;

    @Override
    public void process(Exchange exchange) throws Exception {
        run();
    }
}
