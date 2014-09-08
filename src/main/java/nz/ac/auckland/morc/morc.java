package nz.ac.auckland.morc;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import junit.framework.Test;
import junit.textui.ResultPrinter;
import junit.textui.TestRunner;
import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Suite;
import org.slf4j.LoggerFactory;

/**
 * A convenience class that helps to quickly instantiate test cases using a scripting tool like Groovy.
 * I realize this breaks Java naming convention, but it fits the name used elsewhere better.
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class morc {

    /**
     * A method that will run the builder as a suite of JUnit tests
     *
     * @param builder A set of test specifications that need to be built
     */
    public static void run(MorcTestBuilder builder) {

        JoranConfigurator configurator = new JoranConfigurator();
        LoggerContext context = (LoggerContext)LoggerFactory.getILoggerFactory();
        try {
            configurator.setContext(context);
            context.reset();
            configurator.doConfigure(MorcTestBuilder.classpath("/logback-morc.xml"));
        } catch (JoranException e) {
            throw new RuntimeException(e);
        }

        JUnitCore core = new JUnitCore();
        core.addListener(new TextListener(System.out));
        try {
            core.run(new MorcParameterized(builder));
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}